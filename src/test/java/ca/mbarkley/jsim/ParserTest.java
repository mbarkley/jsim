package ca.mbarkley.jsim;

import ca.mbarkley.jsim.model.Expression;
import ca.mbarkley.jsim.model.Expression.*;
import ca.mbarkley.jsim.model.Question;
import ca.mbarkley.jsim.model.Statement;
import org.junit.Assert;
import org.junit.Test;

import static ca.mbarkley.jsim.model.Expression.Operator.PLUS;
import static ca.mbarkley.jsim.model.Expression.Operator.TIMES;
import static org.assertj.core.api.Assertions.assertThat;

public class ParserTest {
    Parser parser = new Parser();

    @Test
    public void singleDieRoll() {
        final String expression = "d6";

        final Statement result = parser.parse(expression);

        Assert.assertEquals(new HomogeneousDicePool(1, 6), result);
    }

    @Test
    public void multipleDiceRoll() {
        final String expression = "3d8";

        final Statement result = parser.parse(expression);

        Assert.assertEquals(new HomogeneousDicePool(3, 8), result);
    }

    @Test
    public void highDieRoll() {
        final String expression = "3d6H2";

        final Statement result = parser.parse(expression);

        Assert.assertEquals(new Expression.HighDice(new HomogeneousDicePool(3, 6), 2), result);
    }

    @Test
    public void lowDieRoll() {
        final String expression = "3d6L2";

        final Statement result = parser.parse(expression);

        Assert.assertEquals(new Expression.LowDice(new HomogeneousDicePool(3, 6), 2), result);
    }

    @Test
    public void leadingWhitespace() {
        final String expression = " d6";

        final Statement result = parser.parse(expression);

        Assert.assertEquals(new HomogeneousDicePool(1, 6), result);
    }

    @Test
    public void trailingWhitespace() {
        final String expression = "d6 ";

        final Statement result = parser.parse(expression);

        Assert.assertEquals(new HomogeneousDicePool(1, 6), result);
    }

    @Test
    public void singleRollPlusConstant() {
        final String expression = "d6 + 1";

        final Statement result = parser.parse(expression);

        Assert.assertEquals(new BinaryOpExpression(new HomogeneousDicePool(1, 6), PLUS, new Constant(1)), result);
    }

    @Test
    public void singleRollMinusConstant() {
        final String expression = "d6 - 1";

        final Statement result = parser.parse(expression);

        Assert.assertEquals(new BinaryOpExpression(new HomogeneousDicePool(1, 6), Operator.MINUS, new Constant(1)), result);
    }

    @Test
    public void multipleDiceRollPlusConstant() {
        final String expression = "3d8 + 1";

        final Statement result = parser.parse(expression);

        Assert.assertEquals(new BinaryOpExpression(new HomogeneousDicePool(3, 8), PLUS, new Constant(1)), result);
    }

    @Test
    public void multipleDiceRollMinusConstant() {
        final String expression = "3d8 - 1";

        final Statement result = parser.parse(expression);

        Assert.assertEquals(new BinaryOpExpression(new HomogeneousDicePool(3, 8), Operator.MINUS, new Constant(1)), result);
    }

    @Test
    public void multipleDiceRollTimesConstant() {
        final String expression = "3d8 * 2";

        final Statement result = parser.parse(expression);

        Assert.assertEquals(new BinaryOpExpression(new HomogeneousDicePool(3, 8), Operator.TIMES, new Constant(2)), result);
    }

    @Test
    public void multipleDiceRollDivideConstant() {
        final String expression = "3d8 / 2";

        final Statement result = parser.parse(expression);

        Assert.assertEquals(new BinaryOpExpression(new HomogeneousDicePool(3, 8), Operator.DIVIDE, new Constant(2)), result);
    }

    @Test
    public void sumOfDicePools() {
        final String expression = "3d8 + 2d6 + 1";

        final Statement result = parser.parse(expression);

        final BinaryOpExpression expected = new BinaryOpExpression(
                new BinaryOpExpression(new HomogeneousDicePool(3, 8), PLUS, new HomogeneousDicePool(2, 6)),
                PLUS,
                new Constant(1));
        assertThat(result).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void lessThan() {
        final String expression = "3d8 < 2d6";

        final Statement result = parser.parse(expression);

        final Expression left = new HomogeneousDicePool(3, 8);
        final Expression right = new HomogeneousDicePool(2, 6);
        final Question expected = new Question(left, Question.Comparator.LT, right);
        assertThat(result).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void greaterThan() {
        final String expression = "3d8 > 2d6";

        final Statement result = parser.parse(expression);

        final Expression left = new HomogeneousDicePool(3, 8);
        final Expression right = new HomogeneousDicePool(2, 6);
        final Question expected = new Question(left, Question.Comparator.GT, right);
        assertThat(result).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void equalQuestion() {
        final String expression = "3d8 = 2d6";

        final Statement result = parser.parse(expression);

        final Expression left = new HomogeneousDicePool(3, 8);
        final Expression right = new HomogeneousDicePool(2, 6);
        final Question expected = new Question(left, Question.Comparator.EQ, right);
        assertThat(result).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void subtractionThenAddition() {
        final String expression = "2 - 1 + 1";

        final Statement result = parser.parse(expression);

        final Expression expected = new BinaryOpExpression(
                new BinaryOpExpression(new Constant(2), Operator.MINUS, new Constant(1)),
                PLUS,
                new Constant(1)
        );
        assertThat(result).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void bracketed() {
        final String expression = "2 * (2d6 + 1)";

        final Statement result = parser.parse(expression);

        final Expression expected = new BinaryOpExpression(
                new Constant(2),
                TIMES,
                new Bracketed(new BinaryOpExpression(
                        new HomogeneousDicePool(2, 6),
                        PLUS,
                        new Constant(1)
                ))
        );
        assertThat(result).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void complexConstantExpression() {
        final String expression = "3 * (3 + 1) / 4 * 10";

        final Statement result = parser.parse(expression);

        assertThat(result).hasToString(expression);
    }
}
