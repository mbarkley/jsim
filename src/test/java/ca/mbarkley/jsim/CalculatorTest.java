package ca.mbarkley.jsim;

import ca.mbarkley.jsim.model.Question;
import ca.mbarkley.jsim.prob.Event;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class CalculatorTest {
    Calculator calculator = new Calculator(1000);
    Parser parser = new Parser();

    @Test
    public void rollLessThanConstant() {
        final Question question = parser.parseQuestion("d6 < 4");

        final Map<Boolean, Event<Boolean>> result = calculator.calculateResult(question);

        Assert.assertEquals(0.5, result.get(true).getProbability(), 0.0001);
    }

    @Test
    public void complexRollGreaterThanConstant() {
        final Question question = parser.parseQuestion("2d6 + 1 > 6");

        final Map<Boolean, Event<Boolean>> result = calculator.calculateResult(question);

        Assert.assertEquals(0.722, result.get(true).getProbability(), 0.001);
    }

    @Test
    public void complexRollLessThanComplexRoll() {
        final Question question = parser.parseQuestion("2d6 + 1 < 2d8 - 4");

        final Map<Boolean, Event<Boolean>> result = calculator.calculateResult(question);

        Assert.assertEquals(0.20095, result.get(true).getProbability(), 0.00001);
    }

    @Test
    public void constantLessThanConstant() {
        final Question question = parser.parseQuestion("1 < 2");

        final Map<Boolean, Event<Boolean>> result = calculator.calculateResult(question);

        Assert.assertEquals(1.0, result.get(true).getProbability(), 0.00001);
    }

    @Test
    public void bigAdditionQuestion() {
        final Question question = parser.parseQuestion("6d20 + 14d20 > 200");

        final Map<Boolean, Event<Boolean>> result = calculator.calculateResult(question);

        Assert.assertEquals(0.643, result.get(true).getProbability(), 0.001);
    }

    @Test
    public void bigMultiRollQuestion() {
        final Question question = parser.parseQuestion("20d20 > 200");

        final Map<Boolean, Event<Boolean>> result = calculator.calculateResult(question);

        Assert.assertEquals(0.643, result.get(true).getProbability(), 0.001);
    }

    @Test
    public void reallyBigMultiRollQuestion() {
        final Question question = parser.parseQuestion("100d20 > 1000");

        final Map<Boolean, Event<Boolean>> result = calculator.calculateResult(question);

        Assert.assertEquals(0.804, result.get(true).getProbability(), 0.001);
    }
}
