package ca.mbarkley.jsim.eval;

import ca.mbarkley.jsim.antlr.JSimLexer;
import ca.mbarkley.jsim.antlr.JSimParser;
import ca.mbarkley.jsim.antlr.JSimParserBaseVisitor;
import ca.mbarkley.jsim.eval.EvaluationException.InvalidTypeException;
import ca.mbarkley.jsim.eval.EvaluationException.UnknownOperatorException;
import ca.mbarkley.jsim.model.*;
import ca.mbarkley.jsim.model.Expression.*;
import ca.mbarkley.jsim.model.ExpressionConverter.ConverterKey;
import ca.mbarkley.jsim.model.IntegerExpression.HighDice;
import ca.mbarkley.jsim.model.IntegerExpression.HomogeneousDicePool;
import ca.mbarkley.jsim.model.IntegerExpression.LowDice;
import ca.mbarkley.jsim.model.Vector;
import ca.mbarkley.jsim.model.Type.VectorType;
import ca.mbarkley.jsim.prob.Event;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ca.mbarkley.jsim.model.BinaryOperators.lookupBinaryOp;
import static ca.mbarkley.jsim.model.ExpressionConverter.coercionConverters;
import static java.lang.String.format;
import static java.util.stream.Collectors.*;

public class Parser {
    private static final Pattern ROLL = Pattern.compile("(\\d+)?[dD](\\d+)(?:([HhLl])(\\d+))?");

    public Evaluation parse(String expression) {
        return parse(new LexicalScope(Map.of()), expression);
    }

    public Evaluation parse(LexicalScope scope, String expression) {
        final CodePointCharStream is = CharStreams.fromString(expression);
        final JSimLexer lexer = new JSimLexer(is);
        lexer.removeErrorListeners();
        final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        final JSimParser parser = new JSimParser(tokenStream);
        parser.removeErrorListeners();
        parser.setErrorHandler(new BailErrorStrategy());
        final JSimParser.JsimContext ctx = runParser(parser);

        if (ctx.exception != null) {
            throw ctx.exception;
        }

        final StatementVisitor visitor = new StatementVisitor(scope, new ExpressionVisitor());

        return visitor.visit(ctx);
    }

    JSimParser.JsimContext runParser(JSimParser parser) throws RecognitionException {
        try {
            return parser.jsim();
        } catch (ParseCancellationException pce) {
            try {
                throw pce.getCause();
            } catch (RecognitionException re) {
                throw re;
            } catch (Throwable t) {
                throw pce;
            }
        }
    }

    @RequiredArgsConstructor
    private static class StatementVisitor extends JSimParserBaseVisitor<Evaluation> {
        private final LexicalScope initialEvalCtx;
        private final ExpressionVisitor expressionVisitor;

        @Override
        public Evaluation visitJsim(JSimParser.JsimContext ctx) {
            if (ctx.exception != null) {
                throw ctx.exception;
            } else {
                final Map<String, Expression<?>> definitions = new HashMap<>(initialEvalCtx.getDefinitions());
                final List<Expression<?>> expressions = new ArrayList<>();
                for (var stmt : ctx.statement()) {
                    final Evaluation evaluation = visitStatement(new LexicalScope(definitions), stmt);
                    definitions.putAll(evaluation.getContext().getDefinitions());
                    expressions.addAll(evaluation.getExpressions());
                }

                return new Evaluation(new LexicalScope(definitions), expressions);
            }
        }

        public Evaluation visitStatement(LexicalScope scope, JSimParser.StatementContext ctx) {
            if (ctx.expression() != null) {
                return new Evaluation(scope, List.of(expressionVisitor.visitExpression(scope, ctx.expression())));
            } else {
                return visitDefinition(scope, ctx.definition());
            }
        }

        public Evaluation visitDefinition(LexicalScope scope, JSimParser.DefinitionContext ctx) {
            final String identifier = ctx.IDENTIFIER().getText();
            final Expression<?> expression = visitDefinitionBody(scope, ctx.definitionBody(), identifier);

            Map<String, Expression<?>> modifiedDefintions = new HashMap<>(scope.getDefinitions());
            modifiedDefintions.put(identifier, expression);
            final LexicalScope newEvalCtx = new LexicalScope(modifiedDefintions);

            return new Evaluation(newEvalCtx, List.of());
        }

        private Expression<?> visitDefinitionBody(LexicalScope scope, JSimParser.DefinitionBodyContext ctx, String identifier) {
            if (ctx.expression() != null) {
                return expressionVisitor.visitExpression(scope, ctx.expression());
            } else if (ctx.diceDeclaration() != null) {
                return visitDiceDeclaration(scope, ctx.diceDeclaration(), identifier);
            } else {
                throw new IllegalStateException(ctx.getText());
            }
        }

        private Expression<?> visitDiceDeclaration(LexicalScope scope, JSimParser.DiceDeclarationContext ctx, String identifier) {
            final List<Expression<?>> values = ctx.diceSideDeclaration()
                                                .stream()
                                                .map(subCtx -> visitDiceSideDeclaration(scope, subCtx))
                                                .collect(toList());
            final Set<Type<?>> types = values.stream()
                                             .map(Expression::getType)
                                             .collect(toSet());

            if (values.isEmpty()) {
                throw new IllegalStateException("Cannot have empty dice declaration");
            } else {
                final Type<?> targetType = Types.findCommonType(types);
                final List<Expression<?>> newValues =
                        (List) values.stream()
                                     .map(c -> {
                                         final ExpressionConverter converter = coercionConverters.get(new ConverterKey(c.getType().typeClass(), targetType.typeClass()));
                                         final Expression converted = converter.convert(c, targetType);

                                         return converted;
                                     })
                                     .collect(toList());

                return createCustomDie(ctx, identifier, targetType, newValues);
            }
        }

        private Expression<?> createCustomDie(JSimParser.DiceDeclarationContext ctx, String identifier, Type<?> type, List<Expression<?>> values) {
            final List<Event<?>> events = values.stream()
                                                .peek(exp -> {
                                                    if (!exp.isConstant()) {
                                                        throw new IllegalStateException("Can only generate event list for a constant");
                                                    }
                                                })
                                                .flatMap(exp -> exp.calculateResults().values().stream())
                                                .map(e -> new Event<>(e.getValue(), e.getProbability() / values.size()))
                                                .collect(groupingBy(Event::getValue, reducing(0.0, Event::getProbability, Double::sum)))
                                                .entrySet()
                                                .stream()
                                                .map(e -> new Event<>(e.getKey(), e.getValue()))
                                                .collect(toList());

            //noinspection unchecked,rawtypes
            return new CustomDie(identifier, type, events);
        }

        private Expression<?> visitDiceSideDeclaration(LexicalScope scope, JSimParser.DiceSideDeclarationContext ctx) {
            if (ctx.expression() != null) {
                final Expression<?> expression = expressionVisitor.visitExpression(scope, ctx.expression());
                if (expression.isConstant()) {
                    return expression;
                } else {
                    throw new IllegalStateException(format("Cannot use non-constant expressions in dice declaration [%s]", ctx.getText()));
                }
            } else {
                throw new IllegalStateException(ctx.getText());
            }
        }
    }

    @RequiredArgsConstructor
    private static class ExpressionVisitor {
        public Expression<?> visitExpression(LexicalScope scope, JSimParser.ExpressionContext ctx) {
            if (ctx.exception != null) {
                throw ctx.exception;
            } else if (ctx.literal() != null) {
                return visitLiteral(scope, ctx.literal());
            } else if (ctx.reference() != null) {
                return visitReference(scope, ctx.reference());
            } else if (ctx.multiplicativeTerm() != null) {
                return visitMultiplicativeTerm(scope, ctx.multiplicativeTerm());
            } else if (ctx.expression().size() == 1 && ctx.LCB() != null && ctx.RCB() != null && ctx.SYMBOL() != null) {
                return visitVectorComponentRestriction(scope, ctx);
            } else if (ctx.LB() != null && ctx.RB() != null && ctx.expression().size() == 1) {
                return new Bracketed<>(visitExpression(scope, ctx.expression(0)));
            } else if (ctx.expression().size() == 2
                    && ctx.getChildCount() == 3
                    && ctx.getChild(1) instanceof TerminalNode) {
                return visitBinaryExpression(scope, ctx);
            } else if (ctx.letExpression() != null) {
                return visitLetExpression(scope, ctx.letExpression());
            } else {
                throw unsupportedExpression(ctx);
            }
        }

        private Expression<?> visitBinaryExpression(LexicalScope scope, JSimParser.ExpressionContext ctx) {
            final Expression<?> left = visitExpression(scope, ctx.expression(0));
            final Expression<?> right = visitExpression(scope, ctx.expression(1));
            final String operatorSymbol = ctx.getChild(1).getText();

            final Optional<Type<?>> foundCommonType = Types.findCommonType(left.getType(), right.getType());

            return foundCommonType.map(commonType -> {
                final BinaryOperator<?, ?> sign = lookupBinaryOp(commonType, commonType, operatorSymbol)
                        .orElseThrow(() -> new UnknownOperatorException(left.getType(), operatorSymbol, right.getType()));
                final Expression<?> newLeft = Types.convertExpression(left, commonType);
                final Expression<?> newRight = Types.convertExpression(right, commonType);

                return new BinaryOpExpression(newLeft, sign, newRight);
            }).orElseThrow(() -> new UnknownOperatorException(left.getType(), operatorSymbol, right.getType()));
        }

        private Expression<?> visitLetExpression(LexicalScope scope, JSimParser.LetExpressionContext ctx) {
            final Expression<?> bindExpression = visitExpression(scope, ctx.expression(0));
            final String identifier = ctx.IDENTIFIER().getText();
            final Expression<?> valueExpression = visitExpression(scope.with(identifier, new BoundConstant<>(identifier, bindExpression.getType())), ctx.expression(1));

            return new BindExpression<>(identifier, bindExpression, valueExpression);
        }

        private Expression<?> visitVectorComponentRestriction(LexicalScope scope, JSimParser.ExpressionContext ctx) {
            final Expression<Vector> vectorExpression;
            final Symbol symbol;
            final Expression<?> expression = visitExpression(scope, ctx.expression(0));
            if (Types.VECTOR_TYPE_CLASS.equals(expression.getType().typeClass())) {
                vectorExpression = (Expression<Vector>) expression;
            } else {
                throw new InvalidTypeException(format("Expression [%s] in vector component access must be vector type but was [%s]", ctx.expression(0).getText(), expression.getType()));
            }
            if (ctx.SYMBOL() != null) {
                symbol = Symbol.fromText(ctx.SYMBOL().getText());
            } else {
                throw unsupportedExpression(ctx);
            }

            final VectorType vectorType = (VectorType) vectorExpression.getType();
            final Type<?> componentType = vectorType.getDimensions().computeIfAbsent(symbol, s -> {
                throw new IllegalArgumentException(format("Invalid symbol [%s] for vector type [%s]", symbol, vectorType));
            });

            return mapProjection(vectorExpression, symbol, componentType);
        }

        private <T extends Comparable<T>> Expression<T> mapProjection(Expression<Vector> vectorExpression, Symbol symbol, Type<T> componentType) {
            final ExpressionConverter<Vector, T> converter = ExpressionConverter.projectionConverter(symbol, componentType);

            return converter.convert(vectorExpression, componentType);
        }

        private Expression<Vector> visitVectorLiteral(LexicalScope scope, JSimParser.VectorLiteralContext ctx) {
            SortedMap<Symbol, Type<?>> typesByDimName = new TreeMap<>();
            SortedMap<Symbol, Constant<?>> coordinate = new TreeMap<>();
            for (var dim : ctx.dimension()) {
                final Symbol name = Symbol.fromText(dim.SYMBOL().getText());
                final Constant<?> value = visitDimensionValue(scope, dim.dimensionValue());
                typesByDimName.compute(name, (k, v) -> {
                    if (v == null) {
                        return value.getType();
                    } else {
                        throw new EvaluationException.DuplicateDimensionDeclarationException(ctx, k);
                    }
                });
                coordinate.put(name, value);
            }

            final VectorType vectorType = new VectorType(typesByDimName);
            return new Constant<>(vectorType, new Vector(vectorType, coordinate));
        }

        private Constant<?> visitDimensionValue(LexicalScope scope, JSimParser.DimensionValueContext ctx) {
            if (ctx.IDENTIFIER() != null) {
                final Expression<?> identifierValue = lookupIdentifier(scope, ctx.IDENTIFIER().getText());
                if (identifierValue instanceof Constant) {
                    return (Constant<?>) identifierValue;
                } else {
                    throw new EvaluationException(format("Invalid identifier [%s] in vector literal: expected constant but identifier had expression value [%s]", ctx.IDENTIFIER().getText(), identifierValue));
                }
            } else if (ctx.NUMBER() != null) {
                final Integer value = Integer.parseInt(ctx.NUMBER().getText());
                return new Constant<>(Types.INTEGER_TYPE, value);
            } else {
                throw new IllegalStateException(ctx.getText());
            }
        }

        private Expression<?> visitMultiplicativeTerm(LexicalScope scope, JSimParser.MultiplicativeTermContext ctx) {
            final Constant<Integer> number = number(ctx.NUMBER());
            final Expression<Vector> expression;
            if (ctx.reference() != null) {
                final Expression<?> refExpression = visitReference(scope, ctx.reference());
                if (Types.EMPTY_VECTOR_TYPE.isAssignableFrom(refExpression.getType())) {
                    expression = (Expression<Vector>) refExpression;
                } else if (Types.SYMBOL_TYPE_CLASS.isInstance(refExpression.getType())) {
                    final ExpressionConverter converter = coercionConverters.get(new ConverterKey(Types.SYMBOL_TYPE_CLASS, Types.VECTOR_TYPE_CLASS));
                    Type<?> targetType = Types.findCommonType(Set.of(Types.EMPTY_VECTOR_TYPE, refExpression.getType()));
                    expression = converter.convert(refExpression, targetType);
                } else {
                    throw unsupportedExpression(ctx.reference());
                }
            } else if (ctx.SYMBOL() != null) {
                final Expression<Symbol> symbol = symbol(ctx.SYMBOL());
                final ExpressionConverter converter = coercionConverters.get(new ConverterKey(Types.SYMBOL_TYPE_CLASS, Types.VECTOR_TYPE_CLASS));
                Type<?> targetType = Types.findCommonType(Set.of(Types.EMPTY_VECTOR_TYPE, symbol.getType()));
                expression = converter.convert(symbol, targetType);
            } else {
                throw unsupportedExpression(ctx);
            }

            return new MultiplicativeExpression(number.getValue(), expression);
        }

        private Expression<?> visitReference(LexicalScope scope, JSimParser.ReferenceContext ctx) {
            if (ctx.IDENTIFIER() != null) {
                return lookupIdentifier(scope, ctx.IDENTIFIER().getText());
            } else {
                throw unsupportedExpression(ctx);
            }
        }

        private Expression<?> visitLiteral(LexicalScope scope, JSimParser.LiteralContext ctx) {
            if (ctx.booleanLiteral() != null) {
                return visitBooleanLiteral(ctx.booleanLiteral());
            } else if (ctx.vectorLiteral() != null) {
                return visitVectorLiteral(scope, ctx.vectorLiteral());
            } else if (ctx.NUMBER() != null) {
                return number(ctx.NUMBER());
            } else if (ctx.ROLL() != null) {
                final Matcher matcher = ROLL.matcher(ctx.ROLL().getText());
                if (matcher.matches()) {
                    final int numberOfDice = matcher.group(1) == null ?
                            1 :
                            Integer.parseInt(matcher.group(1));
                    final int numberOfSides = Integer.parseInt(matcher.group(2));
                    if (matcher.group(3) == null) {
                        return new HomogeneousDicePool(numberOfDice, numberOfSides);
                    } else {
                        final int maxOrMinDiceNumber = Integer.parseInt(matcher.group(4));
                        if (matcher.group(3).equalsIgnoreCase("L")) {
                            return new LowDice(new HomogeneousDicePool(numberOfDice, numberOfSides), maxOrMinDiceNumber);
                        } else if (matcher.group(3).equalsIgnoreCase("H")) {
                            return new HighDice(new HomogeneousDicePool(numberOfDice, numberOfSides), maxOrMinDiceNumber);
                        }
                    }
                }

                throw new IllegalStateException(format("Unrecognized roll: %s", ctx.ROLL().getText()));
            } else if (ctx.SYMBOL() != null) {
                return symbol(ctx.SYMBOL());
            } else {
                throw unsupportedExpression(ctx);
            }
        }

        private Expression<?> visitBooleanLiteral(JSimParser.BooleanLiteralContext ctx) {
            if (ctx.TRUE() != null) {
                return BooleanExpression.TRUE;
            } else if (ctx.FALSE() != null) {
                return BooleanExpression.FALSE;
            } else {
                throw new IllegalStateException();
            }
        }
    }

    private static Constant<Integer> number(TerminalNode number) {
        return new Constant<>(Types.INTEGER_TYPE, Integer.parseInt(number.getText()));
    }

    private static UnsupportedOperationException unsupportedExpression(RuleContext ctx) {
        return new UnsupportedOperationException(format("Unknown expression kind [%s]", ctx.getText()));
    }

    private static Expression<Symbol> symbol(TerminalNode symbol) {
        final Symbol s = Symbol.fromText(symbol.getText());
        return new Constant<>(Types.symbolTypeOf(s), s);
    }

    static Expression<?> lookupIdentifier(LexicalScope scope, String identifier) {
        final Expression<?> expression = scope.getDefinitions().get(identifier);
        if (expression != null) {
            return expression;
        } else {
            throw new EvaluationException.UndefinedIdentifierException(identifier);
        }
    }
}
