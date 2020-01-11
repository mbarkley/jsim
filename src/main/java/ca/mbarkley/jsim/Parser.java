package ca.mbarkley.jsim;

import ca.mbarkley.jsim.antlr.JSimBaseVisitor;
import ca.mbarkley.jsim.antlr.JSimLexer;
import ca.mbarkley.jsim.antlr.JSimParser;
import ca.mbarkley.jsim.model.BooleanExpression;
import ca.mbarkley.jsim.model.BooleanExpression.*;
import ca.mbarkley.jsim.model.Expression;
import ca.mbarkley.jsim.model.Expression.Bracketed;
import ca.mbarkley.jsim.model.IntegerExpression.*;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;

import java.util.List;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class Parser {
    public List<Expression<?>> parse(String expression) throws RecognitionException {
        final ANTLRInputStream is = new ANTLRInputStream(expression);
        final JSimLexer lexer = new JSimLexer(is);
        lexer.removeErrorListeners();
        final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        final JSimParser parser = new JSimParser(tokenStream);
        parser.removeErrorListeners();
        final JSimParser.JsimContext ctx = parser.jsim();

        if (ctx.exception != null) {
            throw ctx.exception;
        }

        final ArithmeticExpressionVisitor arithmeticExpressionVisitor = new ArithmeticExpressionVisitor();
        final BooleanExpressionVisitor booleanExpressionVisitor = new BooleanExpressionVisitor(arithmeticExpressionVisitor);
        final StatementVisitor visitor = new StatementVisitor(arithmeticExpressionVisitor, booleanExpressionVisitor);

        return visitor.visit(ctx);
    }

    @RequiredArgsConstructor
    private static class StatementVisitor extends JSimBaseVisitor<List<Expression<?>>> {
        private final ArithmeticExpressionVisitor arithmeticExpressionVisitor;
        private final BooleanExpressionVisitor booleanExpressionVisitor;

        @Override
        public List<Expression<?>> visitJsim(JSimParser.JsimContext ctx) {
            return ctx.statement()
                      .stream()
                      .flatMap(stmt -> visitStatement(stmt).stream())
                      .collect(toList());
        }

        @Override
        public List<Expression<?>> visitStatement(JSimParser.StatementContext ctx) {
            if (ctx.exception != null) {
                throw ctx.exception;
            } else if (ctx.booleanExpression() != null) {
                return List.of(booleanExpressionVisitor.visitBooleanExpression(ctx.booleanExpression()));
            } else if (ctx.arithmeticExpression() != null) {
                return List.of(arithmeticExpressionVisitor.visitArithmeticExpression(ctx.arithmeticExpression()));
            } else {
                throw new IllegalArgumentException(format("Statement is neither a question or expression [%s]", ctx));
            }
        }
    }

    @RequiredArgsConstructor
    private static class BooleanExpressionVisitor extends JSimBaseVisitor<Expression<Boolean>> {
        private final ArithmeticExpressionVisitor arithmeticExpressionVisitor;

        @Override
        public Expression<Boolean> visitBooleanExpression(JSimParser.BooleanExpressionContext ctx) {
            if (ctx.exception != null) {
                throw ctx.exception;
            } else if (ctx.booleanExpression().size() == 2) {
                final String booleanOperatorText = ctx.getChild(1).getText();
                final BooleanOperator operator = BooleanOperator.lookup(booleanOperatorText)
                                                                .orElseThrow(() -> new IllegalArgumentException(format("Unrecognized operator [%s]", booleanOperatorText)));

                final Expression<Boolean> left = visitBooleanExpression(ctx.booleanExpression(0));
                final Expression<Boolean> right = visitBooleanExpression(ctx.booleanExpression(1));

                return new BinaryOpBooleanExpression(left, operator, right);
            } else if (ctx.booleanExpression().size() == 1) {
                return new Bracketed<>(visitBooleanExpression(ctx.booleanExpression(0)));
            } else {
                return visitBooleanTerm(ctx.booleanTerm());
            }
        }

        @Override
        public BooleanExpression visitBooleanTerm(JSimParser.BooleanTermContext ctx) {
            if (ctx.booleanLiteral() != null) {
                return visitBooleanLiteral(ctx.booleanLiteral());
            } else {
                final Expression<Integer> left = arithmeticExpressionVisitor.visitArithmeticExpression(ctx.arithmeticExpression(0));
                final Expression<Integer> right = arithmeticExpressionVisitor.visitArithmeticExpression(ctx.arithmeticExpression(1));
                final String comparatorText = ctx.getChild(1).getText();
                final Comparator comparator = Comparator.lookup(comparatorText)
                                                        .orElseThrow(() -> new IllegalArgumentException(format("Unrecognized comparator [%s]", comparatorText)));

                return new ComparisonExpression(left, comparator, right);
            }
        }

        @Override
        public BooleanExpression visitBooleanLiteral(JSimParser.BooleanLiteralContext ctx) {
            if (ctx.TRUE() != null) {
                return BooleanConstant.TRUE;
            } else {
                return BooleanConstant.FALSE;
            }
        }
    }

    private static class ArithmeticExpressionVisitor extends JSimBaseVisitor<Expression<Integer>> {
        @Override
        public Expression<Integer> visitSingleRoll(JSimParser.SingleRollContext ctx) {
            final Token rawNumber = ctx.NUMBER().getSymbol();
            final int dieSides = parseInt(rawNumber.getText());
            return new HomogeneousDicePool(1, dieSides);
        }

        @Override
        public Expression<Integer> visitMultiRoll(JSimParser.MultiRollContext ctx) {
            final int diceNumber = parseInt(ctx.NUMBER(0).getSymbol().getText());
            final int diceSides = parseInt(ctx.NUMBER(1).getSymbol().getText());

            return new HomogeneousDicePool(diceNumber, diceSides);
        }

        @Override
        public Expression<Integer> visitHighRoll(JSimParser.HighRollContext ctx) {
            final int diceNumber = parseInt(ctx.NUMBER(0).getSymbol().getText());
            final int diceSides = parseInt(ctx.NUMBER(1).getSymbol().getText());
            final int highDice = parseInt(ctx.NUMBER(2).getSymbol().getText());
            final HomogeneousDicePool dicePool = new HomogeneousDicePool(diceNumber, diceSides);

            return new HighDice(dicePool, highDice);
        }

        @Override
        public Expression<Integer> visitLowRoll(JSimParser.LowRollContext ctx) {
            final int diceNumber = parseInt(ctx.NUMBER(0).getSymbol().getText());
            final int diceSides = parseInt(ctx.NUMBER(1).getSymbol().getText());
            final int lowDice = parseInt(ctx.NUMBER(2).getSymbol().getText());
            final HomogeneousDicePool dicePool = new HomogeneousDicePool(diceNumber, diceSides);

            return new LowDice(dicePool, lowDice);
        }

        @Override
        public Expression<Integer> visitArithmeticExpression(JSimParser.ArithmeticExpressionContext ctx) {
            if (ctx.exception != null) {
                throw ctx.exception;
            } else if (ctx.arithmeticExpression().size() == 2) {
                return visitBinaryExpression(ctx);
            } else if (ctx.arithmeticExpression().size() == 1) {
                return new Bracketed<>(visitArithmeticExpression(ctx.arithmeticExpression(0)));
            } else if (ctx.arithmeticTerm() != null) {
                return visitArithmeticTerm(ctx.arithmeticTerm());
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public Expression<Integer> visitArithmeticTerm(JSimParser.ArithmeticTermContext ctx) {
            return visitChildren(ctx);
        }

        private Expression<Integer> visitBinaryExpression(JSimParser.ArithmeticExpressionContext ctx) {
            final Expression<Integer> left = visitArithmeticExpression(ctx.arithmeticExpression(0));
            final Expression<Integer> right = visitArithmeticExpression(ctx.arithmeticExpression(1));
            final Operator sign = getOperator(ctx);

            return new BinaryOpExpression(left, sign, right);
        }

        private Operator getOperator(JSimParser.ArithmeticExpressionContext ctx) {
            final String operatorText = ctx.getChild(1).getText();
            return Operator.lookup(operatorText)
                           .orElseThrow(() -> new RuntimeException(format("Unrecognized operator [%s]", operatorText)));
        }

        @Override
        public Expression<Integer> visitNumberLiteral(JSimParser.NumberLiteralContext ctx) {
            return new Constant(parseInt(ctx.NUMBER().getText()));
        }

        @Override
        protected Expression<Integer> aggregateResult(Expression<Integer> aggregate, Expression<Integer> nextResult) {
            if (aggregate == null) {
                return nextResult;
            } else if (nextResult == null) {
                return aggregate;
            } else {
                throw new IllegalStateException(format("Cannot merge two non-null expressions [%s] and [%s] outside context of a binary expression", aggregate, nextResult));
            }
        }
    }
}
