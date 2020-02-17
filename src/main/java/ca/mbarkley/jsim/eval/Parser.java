package ca.mbarkley.jsim.eval;

import ca.mbarkley.jsim.antlr.JSimLexer;
import ca.mbarkley.jsim.antlr.JSimParser;
import ca.mbarkley.jsim.antlr.JSimParserBaseVisitor;
import ca.mbarkley.jsim.eval.EvaluationException.InvalidTypeException;
import ca.mbarkley.jsim.eval.EvaluationException.UnknownOperatorException;
import ca.mbarkley.jsim.model.*;
import ca.mbarkley.jsim.model.BooleanExpression.BooleanOperators;
import ca.mbarkley.jsim.model.BooleanExpression.IntegerComparisons;
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
import static ca.mbarkley.jsim.model.ExpressionConverter.converters;
import static java.lang.String.format;
import static java.util.stream.Collectors.*;

public class Parser {
    private static final Pattern ROLL = Pattern.compile("(\\d+)?[dD](\\d+)(?:([HhLl])(\\d+))?");

    public Evaluation parse(String expression) {
        return parse(new Context(Map.of()), expression);
    }

    public Evaluation parse(Context evalCtx, String expression) {
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

        final ArithmeticExpressionVisitor arithmeticExpressionVisitor = new ArithmeticExpressionVisitor();
        final BooleanExpressionVisitor booleanExpressionVisitor = new BooleanExpressionVisitor(arithmeticExpressionVisitor);
        final StatementVisitor visitor = new StatementVisitor(evalCtx, new ExpressionVisitor(arithmeticExpressionVisitor, booleanExpressionVisitor));

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
        private final Context initialEvalCtx;
        private final ExpressionVisitor expressionVisitor;

        @Override
        public Evaluation visitJsim(JSimParser.JsimContext ctx) {
            if (ctx.exception != null) {
                throw ctx.exception;
            } else {
                final Map<String, Expression<?>> definitions = new HashMap<>(initialEvalCtx.getDefinitions());
                final List<Expression<?>> expressions = new ArrayList<>();
                for (var stmt : ctx.statement()) {
                    final Evaluation evaluation = visitStatement(new Context(definitions), stmt);
                    definitions.putAll(evaluation.getContext().getDefinitions());
                    expressions.addAll(evaluation.getExpressions());
                }

                return new Evaluation(new Context(definitions), expressions);
            }
        }

        public Evaluation visitStatement(Context evalCtx, JSimParser.StatementContext ctx) {
            if (ctx.expression() != null) {
                return new Evaluation(evalCtx, List.of(expressionVisitor.visitExpression(evalCtx, ctx.expression())));
            } else {
                return visitDefinition(evalCtx, ctx.definition());
            }
        }

        public Evaluation visitDefinition(Context evalCtx, JSimParser.DefinitionContext ctx) {
            final String identifier = ctx.IDENTIFIER().getText();
            final Expression<?> expression = visitDefinitionBody(evalCtx, ctx.definitionBody());

            Map<String, Expression<?>> modifiedDefintions = new HashMap<>(evalCtx.getDefinitions());
            modifiedDefintions.put(identifier, expression);
            final Context newEvalCtx = new Context(modifiedDefintions);

            return new Evaluation(newEvalCtx, List.of());
        }

        private Expression<?> visitDefinitionBody(Context evalCtx, JSimParser.DefinitionBodyContext ctx) {
            if (ctx.expression() != null) {
                return expressionVisitor.visitExpression(evalCtx, ctx.expression());
            } else if (ctx.diceDeclaration() != null) {
                return visitDiceDeclaration(evalCtx, ctx.diceDeclaration());
            } else {
                throw new IllegalStateException(ctx.getText());
            }
        }

        private Expression<?> visitDiceDeclaration(Context evalCtx, JSimParser.DiceDeclarationContext ctx) {
            final List<Expression<?>> values = ctx.diceSideDeclaration()
                                                .stream()
                                                .map(subCtx -> visitDiceSideDeclaration(evalCtx, subCtx))
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
                                         final ExpressionConverter converter = converters.get(new ConverterKey(c.getType().typeClass(), targetType.typeClass()));
                                         final Expression converted = converter.convert(c, targetType);

                                         return converted;
                                     })
                                     .collect(toList());

                return generateEventList(ctx, targetType, newValues);
            }
        }

        private Expression<?> generateEventList(JSimParser.DiceDeclarationContext ctx, Type<?> type, List<Expression<?>> values) {
            final List<Event<?>> events = values.stream()
                                                .flatMap(Expression::events)
                                                .map(e -> new Event<>(e.getValue(), e.getProbability() / values.size()))
                                                .collect(groupingBy(Event::getValue, reducing(0.0, Event::getProbability, Double::sum)))
                                                .entrySet()
                                                .stream()
                                                .map(e -> new Event<>(e.getKey(), e.getValue()))
                                                .collect(toList());

            //noinspection unchecked,rawtypes
            return new EventList(type, events);
        }

        private Expression<?> visitDiceSideDeclaration(Context evalCtx, JSimParser.DiceSideDeclarationContext ctx) {
            if (ctx.expression() != null) {
                final Expression<?> expression = expressionVisitor.visitExpression(evalCtx, ctx.expression());
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
        private final ArithmeticExpressionVisitor arithmeticExpressionVisitor;
        private final BooleanExpressionVisitor booleanExpressionVisitor;

        public Expression<?> visitExpression(Context evalCtx, JSimParser.ExpressionContext ctx) {
            if (ctx.exception != null) {
                throw ctx.exception;
            } else if (ctx.booleanExpression() != null) {
                return booleanExpressionVisitor.visitBooleanExpression(evalCtx, ctx.booleanExpression());
            } else if (ctx.arithmeticExpression() != null) {
                return arithmeticExpressionVisitor.visitArithmeticExpression(evalCtx, ctx.arithmeticExpression());
            } else {
                throw unsupportedExpression(ctx);
            }
        }

        public Constant<?> visitVectorLiteral(Context evalCtx, JSimParser.VectorLiteralContext ctx) {
            return arithmeticExpressionVisitor.visitVectorLiteral(evalCtx, ctx);
        }
    }

    @RequiredArgsConstructor
    private static class BooleanExpressionVisitor {
        private final ArithmeticExpressionVisitor arithmeticExpressionVisitor;

        public Expression<Boolean> visitBooleanExpression(Context evalCtx, JSimParser.BooleanExpressionContext ctx) {
            if (ctx.exception != null) {
                throw ctx.exception;
            } else if (ctx.booleanLiteral() != null) {
                return visitBooleanLiteral(evalCtx, ctx.booleanLiteral());
            } else if (ctx.booleanExpression().size() == 2) {
                final String booleanOperatorText = ctx.getChild(1).getText();
                final BinaryOperator<Boolean, Boolean> operator = BooleanOperators.lookup(booleanOperatorText)
                                                                                  .orElseThrow(() -> new IllegalArgumentException(format("Unrecognized operator [%s]", booleanOperatorText)));

                final Expression<Boolean> left = visitBooleanExpression(evalCtx, ctx.booleanExpression(0));
                final Expression<Boolean> right = visitBooleanExpression(evalCtx, ctx.booleanExpression(1));

                return new BinaryOpExpression<>(left, operator, right);
            } else if (ctx.arithmeticExpression().size() == 2 && ctx.EQ() != null) {
                final Expression<?> left = arithmeticExpressionVisitor.visitArithmeticExpression(evalCtx, ctx.arithmeticExpression(0));
                final Expression<?> right = arithmeticExpressionVisitor.visitArithmeticExpression(evalCtx, ctx.arithmeticExpression(1));

                final Optional<Type<?>> foundCommonType = Types.findCommonType(left.getType(), right.getType());

                return foundCommonType.map(commonType -> {
                    final Expression<?> newLeft = Types.convertExpression(left, commonType);
                    final Expression<?> newRight = Types.convertExpression(right, commonType);

                    return new ComparisonExpression(newLeft, BinaryOperator.strictEquality(), newRight);
                }).orElseThrow(() -> new UnknownOperatorException(left.getType(), ctx.EQ().getText(), right.getType()));
            } else if (ctx.booleanExpression().size() == 1) {
                return new Bracketed<>(visitBooleanExpression(evalCtx, ctx.booleanExpression(0)));
            } else if (ctx.arithmeticComparison() != null) {
                return visitArithmeticComparison(evalCtx, ctx.arithmeticComparison());
            } else if (ctx.reference() != null) {
                return visitReference(evalCtx, ctx.reference());
            } else {
                throw new UnsupportedOperationException(format("Unknown part [%s]", ctx.getText()));
            }
        }

        private Expression<Boolean> visitReference(Context evalCtx, JSimParser.ReferenceContext ctx) {
            if (ctx.IDENTIFIER() != null) {
                final Expression<?> expression = lookupIdentifier(evalCtx, ctx.IDENTIFIER().getText());
                if (Types.BOOLEAN_TYPE.isAssignableFrom(expression.getType())) {
                    return (Expression<Boolean>) expression;
                } else {
                    throw new InvalidTypeException(Types.BOOLEAN_TYPE, expression.getType());
                }
            } else {
                throw new UnsupportedOperationException(format("Unknown expression kind [%s]", ctx.getText()));
            }
        }

        public Expression<Boolean> visitArithmeticComparison(Context evalCtx, JSimParser.ArithmeticComparisonContext ctx) {
            if (ctx.arithmeticExpression().size() == 2) {
                final Expression<?> left = arithmeticExpressionVisitor.visitArithmeticExpression(evalCtx, ctx.arithmeticExpression(0));
                final Expression<?> right = arithmeticExpressionVisitor.visitArithmeticExpression(evalCtx, ctx.arithmeticExpression(1));
                final String comparatorText = ctx.getChild(1).getText();
                final BinaryOperator<Integer, Boolean> comparator = IntegerComparisons.lookup(comparatorText)
                                                                                      .orElseThrow(() -> new IllegalArgumentException(format("Unrecognized comparator [%s]", comparatorText)));

                return new ComparisonExpression(left, comparator, right);
            } else {
                throw new IllegalStateException(ctx.getText());
            }
        }

        public Expression<Boolean> visitBooleanLiteral(Context evalCtx, JSimParser.BooleanLiteralContext ctx) {
            if (ctx.TRUE() != null) {
                return BooleanExpression.TRUE;
            } else if (ctx.FALSE() != null) {
                return BooleanExpression.FALSE;
            } else {
                throw new IllegalStateException();
            }
        }
    }

    private static class ArithmeticExpressionVisitor {
        public Expression<?> visitArithmeticExpression(Context evalCtx, JSimParser.ArithmeticExpressionContext ctx) {
            if (ctx.exception != null) {
                throw ctx.exception;
            } else if (ctx.arithmeticExpression().size() == 2) {
                return visitBinaryExpression(evalCtx, ctx);
            } else if (ctx.arithmeticExpression().size() == 1) {
                return new Bracketed<>(visitArithmeticExpression(evalCtx, ctx.arithmeticExpression(0)));
            } else if (ctx.arithmeticLiteral() != null) {
                return visitArithmeticLiteral(evalCtx, ctx.arithmeticLiteral());
            } else if (ctx.reference() != null) {
                return visitReference(evalCtx, ctx.reference());
            } else if (ctx.multiplicativeTerm() != null) {
                return visitMultiplicativeTerm(evalCtx, ctx.multiplicativeTerm());
            } else if (ctx.vectorComponentRestriction() != null) {
                return visitVectorComponentRestriction(evalCtx, ctx.vectorComponentRestriction());
            } else {
                throw unsupportedExpression(ctx);
            }
        }

        private Expression<?> visitVectorComponentRestriction(Context evalCtx, JSimParser.VectorComponentRestrictionContext ctx) {
            final Expression<Vector> vectorExpression;
            final Symbol symbol;
            if (ctx.reference() != null) {
                final Expression<?> refExpression = visitReference(evalCtx, ctx.reference());
                if (refExpression.getType().typeClass().equals(Types.VECTOR_TYPE_CLASS)) {
                    vectorExpression = (Expression<Vector>) refExpression;
                } else {
                    throw new InvalidTypeException(format("Reference [%s] in vector component restriction must be vector type but was [%s]", ctx.reference().getText(), refExpression.getType()));
                }
            } else if (ctx.vectorLiteral() != null) {
                vectorExpression = visitVectorLiteral(evalCtx, ctx.vectorLiteral());
            } else {
                throw unsupportedExpression(ctx);
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
            final List<Event> mapped = vectorExpression.events()
                                                       .map(e -> new Event(e.getValue()
                                                                            .getCoordinate(symbol)
                                                                            .getValue(),
                                                                           e.getProbability()))
                                                       .collect(toList());

            return new EventList(componentType, mapped);
        }

        private Expression<?> visitMultiplicativeTerm(Context evalCtx, JSimParser.MultiplicativeTermContext ctx) {
            final Constant<Integer> number = number(ctx.NUMBER());
            final Expression<Vector> expression;
            if (ctx.reference() != null) {
                final Expression<?> refExpression = visitReference(evalCtx, ctx.reference());
                if (Types.EMPTY_VECTOR_TYPE.isAssignableFrom(refExpression.getType())) {
                    expression = (Expression<Vector>) refExpression;
                } else if (Types.SYMBOL_TYPE_CLASS.isInstance(refExpression.getType())) {
                    expression = symbolToVector((Expression<Symbol>) refExpression);
                } else {
                    throw unsupportedExpression(ctx.reference());
                }
            } else if (ctx.SYMBOL() != null) {
                expression = symbolToVector(symbol(ctx.SYMBOL()));
            } else {
                throw unsupportedExpression(ctx);
            }

            return new MultiplicativeExpression(number.getValue(), expression);
        }

        private Expression<?> visitReference(Context evalCtx, JSimParser.ReferenceContext ctx) {
            if (ctx.IDENTIFIER() != null) {
                return lookupIdentifier(evalCtx, ctx.IDENTIFIER().getText());
            } else {
                throw unsupportedExpression(ctx);
            }
        }

        public Expression<?> visitArithmeticLiteral(Context evalCtx, JSimParser.ArithmeticLiteralContext ctx) {
            if (ctx.exception != null) {
                throw ctx.exception;
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
            } else if (ctx.vectorLiteral() != null) {
                return visitVectorLiteral(evalCtx, ctx.vectorLiteral());
            } else if (ctx.SYMBOL() != null) {
                return symbol(ctx.SYMBOL());
            } else {
                throw new IllegalStateException(ctx.toString());
            }
        }

        private Expression<?> visitBinaryExpression(Context evalCtx, JSimParser.ArithmeticExpressionContext ctx) {
            final Expression<?> left = visitArithmeticExpression(evalCtx, ctx.arithmeticExpression(0));
            final Expression<?> right = visitArithmeticExpression(evalCtx, ctx.arithmeticExpression(1));
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

        private Constant<Vector> visitVectorLiteral(Context evalCtx, JSimParser.VectorLiteralContext ctx) {
            SortedMap<Symbol, Type<?>> typesByDimName = new TreeMap<>();
            SortedMap<Symbol, Constant<?>> coordinate = new TreeMap<>();
            for (var dim : ctx.dimension()) {
                final Symbol name = Symbol.fromText(dim.SYMBOL().getText());
                final Constant<?> value = visitDimensionValue(evalCtx, dim.dimensionValue());
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

        private Constant<?> visitDimensionValue(Context evalCtx, JSimParser.DimensionValueContext ctx) {
            if (ctx.IDENTIFIER() != null) {
                final Expression<?> identifierValue = lookupIdentifier(evalCtx, ctx.IDENTIFIER().getText());
                if (identifierValue instanceof Constant) {
                    return (Constant<?>) identifierValue;
                } else {
                    throw new EvaluationException(format("Invalid identifier [%s] in vector literal: expected constant but identifier had expression value [%s]", ctx.IDENTIFIER().getText(), identifierValue));
                }
            } else if (ctx.NUMBER() != null) {
                // OpenJDK cannot compile with type argument here
                final Integer value = Integer.parseInt(ctx.NUMBER().getText());
                return new Constant<>(Types.INTEGER_TYPE, value);
            } else {
                throw new IllegalStateException(ctx.getText());
            }
        }
    }

    private static Expression<Vector> symbolToVector(final Expression<Symbol> refExpression) {
        final List<Event<Vector>> vectors = refExpression.events()
                                                         .map(symbolEvent -> new Event<>(new Vector(new VectorType(new TreeMap<>(Map.of(symbolEvent.getValue(), Types.INTEGER_TYPE))),
                                                                                                    new TreeMap<>(Map.of(symbolEvent.getValue(), new Constant<>(Types.INTEGER_TYPE, 1)))),
                                                                                         symbolEvent.getProbability()))
                                                         .collect(toList());
        final VectorType type = vectors.stream()
                                       .map(ev -> ev.getValue().getType())
                                       .reduce((v1, v2) -> Types.mergeVectorTypes(List.of(v1, v2)))
                                       .orElse(Types.EMPTY_VECTOR_TYPE);

        return new EventList<>(type, vectors);
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

    static Expression<?> lookupIdentifier(Context evalCtx, String identifier) {
        final Expression<?> expression = evalCtx.getDefinitions().get(identifier);
        if (expression != null) {
            return expression;
        } else {
            throw new EvaluationException.UndefinedIdentifierException(identifier);
        }
    }
}
