package ca.mbarkley.jsim;

import ca.mbarkley.jsim.antlr.JSimBaseVisitor;
import ca.mbarkley.jsim.antlr.JSimLexer;
import ca.mbarkley.jsim.antlr.JSimParser;
import ca.mbarkley.jsim.prob.Expression;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;

public class Parser {
    public Expression parse(String expression) throws RecognitionException {
        final ANTLRInputStream is = new ANTLRInputStream(expression);
        final JSimLexer lexer = new JSimLexer(is);
        final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        final JSimParser parser = new JSimParser(tokenStream);
        final JSimParser.JsimContext ctx = parser.jsim();

        final Visitor visitor = new Visitor();
        return visitor.visit(ctx);
    }

    private class Visitor extends JSimBaseVisitor<Expression> {
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
        public Expression visitExpression(JSimParser.ExpressionContext ctx) {
            final Expression left = visitSimpleExpression(ctx.simpleExpression());
            if (ctx.NUMBER() != null) {
                final Expression.Constant right = new Expression.Constant(parseInt(ctx.NUMBER().getText()));
                final Expression.Operator sign;
                switch (ctx.operator().getText()) {
                    case "+":
                        sign = Expression.Operator.PLUS;
                        break;
                    case "-":
                        sign = Expression.Operator.MINUS;
                        break;
                    default:
                        throw new RuntimeException(format("Unrecognized operator [%s]", ctx.operator().getText()));
                }

                return new Expression.BinaryOpExpression(left, sign, right);
            } else {
                return left;
            }
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
