package ca.mbarkley.jsim;

import ca.mbarkley.jsim.prob.RandomDicePoolVariable;
import ca.mbarkley.jsim.prob.RandomDieVariable;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ParserTest {
    Parser parser = new Parser();

    @Test
    public void singleDieRoll() {
        final String expression = "d6";

        final RandomDicePoolVariable result = parser.parse(expression);

        Assert.assertEquals(new RandomDicePoolVariable(List.of(new RandomDieVariable(6)), 0), result);
    }

    @Test
    public void multipleDiceRoll() {
        final String expression = "3d8";

        final RandomDicePoolVariable result = parser.parse(expression);

        final RandomDieVariable d8 = new RandomDieVariable(8);
        Assert.assertEquals(new RandomDicePoolVariable(List.of(d8, d8, d8), 0), result);
    }

    @Test
    public void leadingWhitespace() {
        final String expression = " d6";

        final RandomDicePoolVariable result = parser.parse(expression);

        Assert.assertEquals(new RandomDicePoolVariable(List.of(new RandomDieVariable(6)), 0), result);
    }

    @Test
    public void trailingWhitespace() {
        final String expression = "d6 ";

        final RandomDicePoolVariable result = parser.parse(expression);

        Assert.assertEquals(new RandomDicePoolVariable(List.of(new RandomDieVariable(6)), 0), result);
    }

    @Test
    public void singleRollPlusConstant() {
        final String expression = "d6 + 1";

        final RandomDicePoolVariable result = parser.parse(expression);

        Assert.assertEquals(new RandomDicePoolVariable(List.of(new RandomDieVariable(6)), 1), result);
    }

    @Test
    public void singleRollMinusConstant() {
        final String expression = "d6 - 1";

        final RandomDicePoolVariable result = parser.parse(expression);

        Assert.assertEquals(new RandomDicePoolVariable(List.of(new RandomDieVariable(6)), -1), result);
    }

    @Test
    public void multipleDiceRollPlusConstant() {
        final String expression = "3d8 + 1";

        final RandomDicePoolVariable result = parser.parse(expression);

        final RandomDieVariable d8 = new RandomDieVariable(8);
        Assert.assertEquals(new RandomDicePoolVariable(List.of(d8, d8, d8), 1), result);
    }

    @Test
    public void multipleDiceRollMinusConstant() {
        final String expression = "3d8 - 1";

        final RandomDicePoolVariable result = parser.parse(expression);

        final RandomDieVariable d8 = new RandomDieVariable(8);
        Assert.assertEquals(new RandomDicePoolVariable(List.of(d8, d8, d8), -1), result);
    }
}
