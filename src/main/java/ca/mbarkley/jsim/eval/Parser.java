package ca.mbarkley.jsim.eval;

import ca.mbarkley.jsim.antlr.JSimLexer;
import ca.mbarkley.jsim.antlr.JSimParser;
import ca.mbarkley.jsim.antlr.JSimParserBaseVisitor;
import ca.mbarkley.jsim.eval.EvaluationException.UnknownOperatorException;
import ca.mbarkley.jsim.model.*;
import ca.mbarkley.jsim.model.BooleanExpression.BooleanOperators;
import ca.mbarkley.jsim.model.BooleanExpression.IntegerComparisons;
import ca.mbarkley.jsim.model.Expression.*;
import ca.mbarkley.jsim.model.IntegerExpression.HighDice;
import ca.mbarkley.jsim.model.IntegerExpression.HomogeneousDicePool;
import ca.mbarkley.jsim.model.IntegerExpression.LowDice;
import ca.mbarkley.jsim.model.Vector;
import ca.mbarkley.jsim.model.Type.VectorType;
import ca.mbarkley.jsim.prob.Event;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ca.mbarkley.jsim.model.ArithmeticOperators.lookupBinaryOp;
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
            final List<Constant<?>> values = ctx.diceSideDeclaration()
                                                .stream()
                                                .map(subCtx -> visitDiceSideDeclaration(evalCtx, subCtx))
                                                .collect(toList());
            final Set<Type<?>> types = values.stream()
                                                       .map(Constant::getType)
                                                       .collect(toSet());

            if (values.isEmpty()) {
                throw new IllegalStateException("Cannot have empty dice declaration");
            } else {
                final Type<?> type = Types.findCommonType(types);
                if (type instanceof VectorType) {
                    final SortedMap<Symbol, Type<?>> dimensions = ((VectorType) type).getDimensions();
                    final List<Constant<?>> newValues =
                            values.stream()
                                  .map(c -> {
                                      final Vector v = (Vector) c.getValue();
                                      final SortedMap<Symbol, Constant<?>> coordinate = v.getCoordinate();
                                      final SortedMap<Symbol, Constant<?>> newCoordinate = new TreeMap<>(coordinate);
                                      for (var entry : dimensions.entrySet()) {
                                          final Symbol name = entry.getKey();
                                          final Type<?> dimType = entry.getValue();
                                          if (!coordinate.containsKey(name)) {
                                              final Comparable<?> zero = dimType.zero();
                                              newCoordinate.put(name, new Constant(dimType, zero));
                                          }
                                      }

                                      return new Constant<>((VectorType) type, new Vector((VectorType) type, newCoordinate));
                                  })
                                  .collect(toList());

                    return generateEventList(ctx, type, newValues);
                } else {
                    return generateEventList(ctx, type, values);
                }
            }
        }

        private Expression<?> generateEventList(JSimParser.DiceDeclarationContext ctx, Type<?> type, List<Constant<?>> values) {
            final double prob = 1.0 / (double) ctx.diceSideDeclaration().size();
            final List<Event<?>> events = values.stream()
                                                .map(Constant::getValue)
                                                .map(v -> new Event<>(v, prob))
                                                .collect(groupingBy(Event::getValue, reducing(0.0, Event::getProbability, Double::sum)))
                                                .entrySet()
                                                .stream()
                                                .map(e -> new Event<>(e.getKey(), e.getValue()))
                                                .collect(toList());

            //noinspection unchecked,rawtypes
            return new EventList(type, events);
        }

        private Constant<?> visitDiceSideDeclaration(Context evalCtx, JSimParser.DiceSideDeclarationContext ctx) {
            if (ctx.NUMBER() != null) {
                return new Constant<>(Types.INTEGER_TYPE, Integer.parseInt(ctx.NUMBER().getText()));
            } else if (ctx.TRUE() != null) {
                return BooleanExpression.TRUE;
            } else if (ctx.FALSE() != null) {
                return BooleanExpression.FALSE;
            } else if (ctx.SYMBOL() != null) {
                return new Constant<>(Types.SYMBOL_TYPE, Symbol.fromText(ctx.SYMBOL().getText()));
            } else if (ctx.vectorLiteral() != null) {
                return expressionVisitor.visitVectorLiteral(evalCtx, ctx.vectorLiteral());
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
            } else if (ctx.IDENTIFIER() != null) {
                final String identifier = ctx.IDENTIFIER().getText();
                return lookupIdentifier(evalCtx, identifier);
            } else if (ctx.SYMBOL() != null) {
                return new Constant<>(Types.SYMBOL_TYPE, Symbol.fromText(ctx.SYMBOL().getText()));
            } else {
                throw new IllegalArgumentException(format("Unknown expression kind [%s]", ctx.getText()));
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
            } else if (ctx.booleanTerm() != null) {
                return visitBooleanTerm(evalCtx, ctx.booleanTerm());
            } else if (ctx.symbolTerm().size() == 2) {
                final Expression<Symbol> left = visitSymbolTerm(evalCtx, ctx.symbolTerm(0));
                final Expression<Symbol> right = visitSymbolTerm(evalCtx, ctx.symbolTerm(1));

                return new ComparisonExpression<>(left, BinaryOperator.equality(), right);
            } else if (ctx.booleanExpression().size() == 2) {
                final String booleanOperatorText = ctx.getChild(1).getText();
                final BinaryOperator<Boolean, Boolean> operator = BooleanOperators.lookup(booleanOperatorText)
                                                                                  .orElseThrow(() -> new IllegalArgumentException(format("Unrecognized operator [%s]", booleanOperatorText)));

                final Expression<Boolean> left = visitBooleanExpression(evalCtx, ctx.booleanExpression(0));
                final Expression<Boolean> right = visitBooleanExpression(evalCtx, ctx.booleanExpression(1));

                return new BinaryOpExpression<>(Types.BOOLEAN_TYPE, left, operator, right);
            } else if (ctx.booleanExpression().size() == 1) {
                return new Bracketed<>(visitBooleanExpression(evalCtx, ctx.booleanExpression(0)));
            } else {
                return visitArithmeticComparison(evalCtx, ctx.arithmeticComparison());
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

        private Expression<Symbol> visitSymbolTerm(Context evalCtx, JSimParser.SymbolTermContext ctx) {
            if (ctx.SYMBOL() != null) {
                return new Constant<>(Types.SYMBOL_TYPE, Symbol.fromText(ctx.getText()));
            } else if (ctx.IDENTIFIER() != null) {
                final Expression<?> expression = lookupIdentifier(evalCtx, ctx.IDENTIFIER().getText());
                return Types.SYMBOL_TYPE.asType(expression);
            } else {
                throw new IllegalStateException(ctx.getText());
            }
        }

        public Expression<Boolean> visitBooleanTerm(Context evalCtx, JSimParser.BooleanTermContext ctx) {
            if (ctx.TRUE() != null) {
                return BooleanExpression.TRUE;
            } else if (ctx.FALSE() != null) {
                return BooleanExpression.FALSE;
            } else if (ctx.IDENTIFIER() != null) {
                final String identifier = ctx.IDENTIFIER().getText();
                final Expression<?> expression = lookupIdentifier(evalCtx, identifier);
                return Types.BOOLEAN_TYPE.asType(expression);
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
            } else if (ctx.arithmeticTerm() != null) {
                return visitArithmeticTerm(evalCtx, ctx.arithmeticTerm());
            } else {
                throw new IllegalStateException();
            }
        }

        public Expression<?> visitArithmeticTerm(Context evalCtx, JSimParser.ArithmeticTermContext ctx) {
            if (ctx.exception != null) {
                throw ctx.exception;
            } else if (ctx.NUMBER() != null) {
                return new Constant<>(Types.INTEGER_TYPE, Integer.parseInt(ctx.NUMBER().getText()));
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
            } else if (ctx.IDENTIFIER() != null) {
                final String identifier = ctx.IDENTIFIER().getText();
                return lookupIdentifier(evalCtx, identifier);
            } else if (ctx.vectorLiteral() != null) {
                return visitVectorLiteral(evalCtx, ctx.vectorLiteral());
            } else {
                throw new IllegalStateException(ctx.toString());
            }
        }

        private Expression<?> visitBinaryExpression(Context evalCtx, JSimParser.ArithmeticExpressionContext ctx) {
            final Expression<?> left = visitArithmeticExpression(evalCtx, ctx.arithmeticExpression(0));
            final Expression<?> right = visitArithmeticExpression(evalCtx, ctx.arithmeticExpression(1));
            final String operatorSymbol = ctx.getChild(1).getText();
            final BinaryOperator<?, ?> sign = lookupBinaryOp(left.getType(), right.getType(), operatorSymbol)
                    .orElseThrow(() -> new UnknownOperatorException(left.getType(), operatorSymbol, right.getType()));

            return new BinaryOpExpression(sign.unsafeGetOuptutType(left.getType(), right.getType()), left, sign, right);
        }

        private Constant<?> visitVectorLiteral(Context evalCtx, JSimParser.VectorLiteralContext ctx) {
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

    static Expression<?> lookupIdentifier(Context evalCtx, String identifier) {
        final Expression<?> expression = evalCtx.getDefinitions().get(identifier);
        if (expression != null) {
            return expression;
        } else {
            throw new EvaluationException.UndefinedIdentifierException(identifier);
        }
    }
}
