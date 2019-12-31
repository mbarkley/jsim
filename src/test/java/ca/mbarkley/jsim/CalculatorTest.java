package ca.mbarkley.jsim;

import ca.mbarkley.jsim.model.Question;
import ca.mbarkley.jsim.prob.Calculator;
import org.junit.Assert;
import org.junit.Test;

public class CalculatorTest {
    Calculator calculator = new Calculator();
    Parser parser = new Parser();

    @Test
    public void rollLessThanConstant() {
        final Question question = parser.parseQuestion("d6 < 4");

        final double result = calculator.calculateProbability(question);

        Assert.assertEquals(0.5, result, 0.0001);
    }
}
