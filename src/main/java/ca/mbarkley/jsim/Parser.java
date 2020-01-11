package ca.mbarkley.jsim;

import ca.mbarkley.jsim.antlr.JSimBaseVisitor;
import ca.mbarkley.jsim.antlr.JSimLexer;
import ca.mbarkley.jsim.antlr.JSimParser;
import ca.mbarkley.jsim.model.Expression;
import ca.mbarkley.jsim.model.Expression.BinaryOpExpression;
import ca.mbarkley.jsim.model.Expression.Bracketed;
import ca.mbarkley.jsim.model.Expression.Operator;
import ca.mbarkley.jsim.model.PrecedenceRotator;
import ca.mbarkley.jsim.model.Question;
import ca.mbarkley.jsim.model.Question.*;
import ca.mbarkley.jsim.model.Statement;
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
    public List<Statement<?>> parse(String expression) throws RecognitionException {
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
    private static class StatementVisitor extends JSimBaseVisitor<List<Statement<?>>> {
        private final ArithmeticExpressionVisitor arithmeticExpressionVisitor;
        private final BooleanExpressionVisitor booleanExpressionVisitor;

        @Override
        public List<Statement<?>> visitJsim(JSimParser.JsimContext ctx) {
            return ctx.statement()
                      .stream()
                      .flatMap(stmt -> visitStatement(stmt).stream())
                      .collect(toList());
        }

        @Override
        public List<Statement<?>> visitStatement(JSimParser.StatementContext ctx) {
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
    private static class BooleanExpressionVisitor extends JSimBaseVisitor<Question> {
        private final ArithmeticExpressionVisitor arithmeticExpressionVisitor;

        @Override
        public Question visitBooleanExpression(JSimParser.BooleanExpressionContext ctx) {
            if (ctx.exception != null) {
                throw ctx.exception;
            } else if (ctx.booleanExpression() == null) {
                return visitBooleanTerm(ctx.booleanTerm());
            } else {
                final String booleanOperatorText = ctx.getChild(1).getText();
                final BooleanOperator operator = BooleanOperator.lookup(booleanOperatorText)
                                                                .orElseThrow(() -> new IllegalArgumentException(format("Unrecognized operator [%s]", booleanOperatorText)));
                final PrecedenceRotator<BooleanOperator, Question, BinaryOpQuestion> rotator = PrecedenceRotator.binaryOpQuestionRotator();
                final JSimParser.BooleanTermContext left = ctx.booleanTerm();
                final Question right = visitBooleanExpression(ctx.booleanExpression());

                final BinaryOpQuestion question = new BinaryOpQuestion(visitBooleanTerm(left), operator, right);

                return rotator.maybeRotate(question).orElse(question);
            }
        }

        @Override
        public Question visitBooleanTerm(JSimParser.BooleanTermContext ctx) {
            if (ctx.booleanLiteral() != null) {
                return visitBooleanLiteral(ctx.booleanLiteral());
            } else {
                final Expression left = arithmeticExpressionVisitor.visitArithmeticExpression(ctx.arithmeticExpression(0));
                final Expression right = arithmeticExpressionVisitor.visitArithmeticExpression(ctx.arithmeticExpression(1));
                final String comparatorText = ctx.getChild(1).getText();
                final Comparator comparator = Comparator.lookup(comparatorText)
                                                        .orElseThrow(() -> new IllegalArgumentException(format("Unrecognized comparator [%s]", comparatorText)));

                return new BinaryBooleanExpression(left, comparator, right);
            }
        }

        @Override
        public Question visitBooleanLiteral(JSimParser.BooleanLiteralContext ctx) {
            if (ctx.TRUE() != null) {
                return BooleanConstant.TRUE;
            } else {
                return BooleanConstant.FALSE;
            }
        }
    }

    private static class ArithmeticExpressionVisitor extends JSimBaseVisitor<Expression> {
        @Override
        public Expression visitSingleRoll(JSimParser.SingleRollContext ctx) {
            final Token rawNumber = ctx.NUMBER().getSymbol();
            final int dieSides = parseInt(rawNumber.getText());
            return new Expression.HomogeneousDicePool(1, dieSides);
        }

        @Override
        public Expression visitMultiRoll(JSimParser.MultiRollContext ctx) {
            final int diceNumber = parseInt(ctx.NUMBER(0).getSymbol().getText());
            final int diceSides = parseInt(ctx.NUMBER(1).getSymbol().getText());

            return new Expression.HomogeneousDicePool(diceNumber, diceSides);
        }

        @Override
        public Expression visitHighRoll(JSimParser.HighRollContext ctx) {
            final int diceNumber = parseInt(ctx.NUMBER(0).getSymbol().getText());
            final int diceSides = parseInt(ctx.NUMBER(1).getSymbol().getText());
            final int highDice = parseInt(ctx.NUMBER(2).getSymbol().getText());
            final Expression.HomogeneousDicePool dicePool = new Expression.HomogeneousDicePool(diceNumber, diceSides);

            return new Expression.HighDice(dicePool, highDice);
        }

        @Override
        public Expression visitLowRoll(JSimParser.LowRollContext ctx) {
            final int diceNumber = parseInt(ctx.NUMBER(0).getSymbol().getText());
            final int diceSides = parseInt(ctx.NUMBER(1).getSymbol().getText());
            final int lowDice = parseInt(ctx.NUMBER(2).getSymbol().getText());
            final Expression.HomogeneousDicePool dicePool = new Expression.HomogeneousDicePool(diceNumber, diceSides);

            return new Expression.LowDice(dicePool, lowDice);
        }

        @Override
        public Expression visitArithmeticExpression(JSimParser.ArithmeticExpressionContext ctx) {
            if (ctx.arithmeticExpression() != null) {
                return visitBinaryExpression(ctx);
            } else if (ctx.arithmeticTerm() != null && ctx.arithmeticExpression() == null) {
                return visitArithmeticTerm(ctx.arithmeticTerm());
            } else if (ctx.exception != null) {
                throw ctx.exception;
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public Expression visitArithmeticTerm(JSimParser.ArithmeticTermContext ctx) {
            if (ctx.arithmeticExpression() != null) {
                return new Bracketed(visitArithmeticExpression(ctx.arithmeticExpression()));
            } else {
                return visitChildren(ctx);
            }
        }

        private Expression visitBinaryExpression(JSimParser.ArithmeticExpressionContext ctx) {
            final Expression left = visitArithmeticTerm(ctx.arithmeticTerm());
            final Expression right = visitArithmeticExpression(ctx.arithmeticExpression());
            final Operator sign = getOperator(ctx);
            final BinaryOpExpression combined = new BinaryOpExpression(left, sign, right);
            final PrecedenceRotator<Operator, Expression, BinaryOpExpression> rotator = PrecedenceRotator.binaryOpExpressionRotator();

            return rotator.maybeRotate(combined).orElse(combined);
        }

        private Operator getOperator(JSimParser.ArithmeticExpressionContext ctx) {
            final String operatorText = ctx.getChild(1).getText();
            return Operator.lookup(operatorText)
                           .orElseThrow(() -> new RuntimeException(format("Unrecognized operator [%s]", operatorText)));
        }

        @Override
        public Expression visitNumberLiteral(JSimParser.NumberLiteralContext ctx) {
            return new Expression.Constant(parseInt(ctx.NUMBER().getText()));
        }

        @Override
        protected Expression aggregateResult(Expression aggregate, Expression nextResult) {
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
