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
import ca.mbarkley.jsim.model.Question.BinaryOpQuestion;
import ca.mbarkley.jsim.model.Question.BooleanOperator;
import ca.mbarkley.jsim.model.Question.Comparator;
import ca.mbarkley.jsim.model.Question.Predicate;
import ca.mbarkley.jsim.model.Statement;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;

public class Parser {
    public Question parseQuestion(String expression) throws RecognitionException {
        final Statement<?> parsed = parse(expression);

        return (Question) parsed;
    }

    public Expression parseExpression(String expression) throws RecognitionException {
        final Statement<?> parsed = parse(expression);

        return (Expression) parsed;
    }

    public Statement<?> parse(String expression) throws RecognitionException {
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

        final ExpressionVisitor expressionVisitor = new ExpressionVisitor();
        final QuestionVisitor questionVisitor = new QuestionVisitor(expressionVisitor);
        final StatementVisitor visitor = new StatementVisitor(expressionVisitor, questionVisitor);

        return visitor.visit(ctx);
    }

    @RequiredArgsConstructor
    private static class StatementVisitor extends JSimBaseVisitor<Statement> {
        private final ExpressionVisitor expressionVisitor;
        private final QuestionVisitor questionVisitor;

        @Override
        public Statement visitJsim(JSimParser.JsimContext ctx) {
            if (ctx.question() != null) {
                return questionVisitor.visitQuestion(ctx.question());
            } else if (ctx.expression() != null) {
                return expressionVisitor.visitExpression(ctx.expression());
            } else {
                throw new IllegalArgumentException(format("Statement is neither a question or expression [%s]", ctx.getText()));
            }
        }
    }

    @RequiredArgsConstructor
    private static class QuestionVisitor extends JSimBaseVisitor<Question> {
        private final ExpressionVisitor expressionVisitor;

        @Override
        public Question visitQuestion(JSimParser.QuestionContext ctx) {
            if (ctx.question() == null) {
                return visitPredicate(ctx.predicate());
            } else {
                final String booleanOperatorText = ctx.getChild(1).getText();
                final BooleanOperator operator = BooleanOperator.lookup(booleanOperatorText)
                                                                .orElseThrow(() -> new IllegalArgumentException(format("Unrecognized operator [%s]", booleanOperatorText)));
                final PrecedenceRotator<BooleanOperator, Question, BinaryOpQuestion> rotator = PrecedenceRotator.binaryOpQuestionRotator();
                final JSimParser.PredicateContext left = ctx.predicate();
                final Question right = visitQuestion(ctx.question());

                final BinaryOpQuestion question = new BinaryOpQuestion(visitPredicate(left), operator, right);

                return rotator.maybeRotate(question).orElse(question);
            }
        }

        @Override
        public Question visitPredicate(JSimParser.PredicateContext ctx) {
            final Expression left = expressionVisitor.visitExpression(ctx.expression(0));
            final Expression right = expressionVisitor.visitExpression(ctx.expression(1));
            final String comparatorText = ctx.getChild(1).getText();
            final Comparator comparator = Comparator.lookup(comparatorText)
                                                    .orElseThrow(() -> new IllegalArgumentException(format("Unrecognized comparator [%s]", comparatorText)));

            return new Predicate(left, comparator, right);
        }
    }

    private static class ExpressionVisitor extends JSimBaseVisitor<Expression> {
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
        public Expression visitExpression(JSimParser.ExpressionContext ctx) {
            if (ctx.expression() != null) {
                return visitBinaryExpression(ctx);
            } else if (ctx.term() != null && ctx.expression() == null) {
                return visitTerm(ctx.term());
            } else if (ctx.exception != null) {
                throw ctx.exception;
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public Expression visitTerm(JSimParser.TermContext ctx) {
            if (ctx.expression() != null) {
                return new Bracketed(visitExpression(ctx.expression()));
            } else if (ctx.atom() != null) {
                return visitAtom(ctx.atom());
            } else {
                throw new IllegalStateException();
            }
        }

        private Expression visitBinaryExpression(JSimParser.ExpressionContext ctx) {
            final Expression left = visitTerm(ctx.term());
            final Expression right = visitExpression(ctx.expression());
            final Operator sign = getOperator(ctx);
            final BinaryOpExpression combined = new BinaryOpExpression(left, sign, right);
            final PrecedenceRotator<Operator, Expression, BinaryOpExpression> rotator = PrecedenceRotator.binaryOpExpressionRotator();

            return rotator.maybeRotate(combined).orElse(combined);
        }

        private Operator getOperator(JSimParser.ExpressionContext ctx) {
            final String operatorText = ctx.getChild(1).getText();
            return Operator.lookup(operatorText)
                           .orElseThrow(() -> new RuntimeException(format("Unrecognized operator [%s]", operatorText)));
        }

        @Override
        public Expression visitConstant(JSimParser.ConstantContext ctx) {
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
