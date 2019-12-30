package ca.mbarkley.jsim;

import ca.mbarkley.jsim.prob.Expression;
import org.junit.Assert;
import org.junit.Test;

public class ParserTest {
    Parser parser = new Parser();

    @Test
    public void singleDieRoll() {
        final String expression = "d6";

        final Expression result = parser.parse(expression);

        Assert.assertEquals(new Expression.HomogeneousDicePool(1, 6), result);
    }

    @Test
    public void multipleDiceRoll() {
        final String expression = "3d8";

        final Expression result = parser.parse(expression);

        Assert.assertEquals(new Expression.HomogeneousDicePool(3, 8), result);
    }

    @Test
    public void leadingWhitespace() {
        final String expression = " d6";

        final Expression result = parser.parse(expression);

        Assert.assertEquals(new Expression.HomogeneousDicePool(1, 6), result);
    }

    @Test
    public void trailingWhitespace() {
        final String expression = "d6 ";

        final Expression result = parser.parse(expression);

        Assert.assertEquals(new Expression.HomogeneousDicePool(1, 6), result);
    }

    @Test
    public void singleRollPlusConstant() {
        final String expression = "d6 + 1";

        final Expression result = parser.parse(expression);

        Assert.assertEquals(new Expression.BinaryOpExpression(new Expression.HomogeneousDicePool(1, 6), Expression.Operator.PLUS, new Expression.Constant(1)), result);
    }

    @Test
    public void singleRollMinusConstant() {
        final String expression = "d6 - 1";

        final Expression result = parser.parse(expression);

        Assert.assertEquals(new Expression.BinaryOpExpression(new Expression.HomogeneousDicePool(1, 6), Expression.Operator.MINUS, new Expression.Constant(1)), result);
    }

    @Test
    public void multipleDiceRollPlusConstant() {
        final String expression = "3d8 + 1";

        final Expression result = parser.parse(expression);

        Assert.assertEquals(new Expression.BinaryOpExpression(new Expression.HomogeneousDicePool(3, 8), Expression.Operator.PLUS, new Expression.Constant(1)), result);
    }

    @Test
    public void multipleDiceRollMinusConstant() {
        final String expression = "3d8 - 1";

        final Expression result = parser.parse(expression);

        Assert.assertEquals(new Expression.BinaryOpExpression(new Expression.HomogeneousDicePool(3, 8), Expression.Operator.MINUS, new Expression.Constant(1)), result);
    }

    @Test
    public void sumOfDicePools() {
        final String expression = "3d8 + 2d6 + 1";

        final Expression result = parser.parse(expression);

        final Expression.HomogeneousDicePool d8s = new Expression.HomogeneousDicePool(3, 8);
        final Expression subExpression = new Expression.BinaryOpExpression(new Expression.HomogeneousDicePool(2, 6), Expression.Operator.PLUS, new Expression.Constant(1));
        final Expression.BinaryOpExpression expected = new Expression.BinaryOpExpression(d8s, Expression.Operator.PLUS, subExpression);
        Assert.assertEquals(expected, result);
    }
}
