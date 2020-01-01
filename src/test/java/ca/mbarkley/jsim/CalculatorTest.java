package ca.mbarkley.jsim;

import ca.mbarkley.jsim.model.Expression;
import ca.mbarkley.jsim.model.Question;
import ca.mbarkley.jsim.prob.Event;
import org.assertj.core.data.Offset;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

public class CalculatorTest {
    Calculator calculator = new Calculator();
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
    public void multipliedRollsGreaterThanConstant() {
        final Question question = parser.parseQuestion("d4 * d4 > 8");

        final Map<Boolean, Event<Boolean>> result = calculator.calculateResult(question);

        Assert.assertEquals(0.250, result.get(true).getProbability(), 0.001);
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

    @Test
    public void simpleExpressionResults() {
        final Expression expression = parser.parseExpression("2d4");

        final Map<Integer, Double> result = calculator.calculateResult(expression)
                                                      .entrySet()
                                                      .stream()
                                                      .collect(toMap(Map.Entry::getKey, e -> e.getValue().getProbability()));

        final Offset<Double> offset = offset(0.0001);
        assertThat(result).hasEntrySatisfying(2, prob -> assertThat(prob).isCloseTo(1.0 / 16.0, offset))
                          .hasEntrySatisfying(3, prob -> assertThat(prob).isCloseTo(2.0 / 16.0, offset))
                          .hasEntrySatisfying(4, prob -> assertThat(prob).isCloseTo(3.0 / 16.0, offset))
                          .hasEntrySatisfying(5, prob -> assertThat(prob).isCloseTo(4.0 / 16.0, offset))
                          .hasEntrySatisfying(6, prob -> assertThat(prob).isCloseTo(3.0 / 16.0, offset))
                          .hasEntrySatisfying(7, prob -> assertThat(prob).isCloseTo(2.0 / 16.0, offset))
                          .hasEntrySatisfying(8, prob -> assertThat(prob).isCloseTo(1.0 / 16.0, offset))
                          .containsOnlyKeys(2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    public void highDiceExpressionResults() {
        final Expression expression = parser.parseExpression("7d4H1");

        final Map<Integer, Double> result = calculator.calculateResult(expression)
                                                      .entrySet()
                                                      .stream()
                                                      .collect(toMap(Map.Entry::getKey, e -> e.getValue().getProbability()));

        final Offset<Double> offset = offset(0.00001);
        assertThat(result).hasEntrySatisfying(1, prob -> assertThat(prob).isCloseTo(0.00006, offset))
                          .hasEntrySatisfying(2, prob -> assertThat(prob).isCloseTo(0.00775, offset))
                          .hasEntrySatisfying(3, prob -> assertThat(prob).isCloseTo(0.12567, offset))
                          .hasEntrySatisfying(4, prob -> assertThat(prob).isCloseTo(0.86652, offset))
                          .containsOnlyKeys(1, 2, 3, 4);
    }

    @Test
    public void lowDiceExpressionResults() {
        final Expression expression = parser.parseExpression("7d4L1");

        final Map<Integer, Double> result = calculator.calculateResult(expression)
                                                      .entrySet()
                                                      .stream()
                                                      .collect(toMap(Map.Entry::getKey, e -> e.getValue().getProbability()));

        final Offset<Double> offset = offset(0.00001);
        assertThat(result).hasEntrySatisfying(4, prob -> assertThat(prob).isCloseTo(0.00006, offset))
                          .hasEntrySatisfying(3, prob -> assertThat(prob).isCloseTo(0.00775, offset))
                          .hasEntrySatisfying(2, prob -> assertThat(prob).isCloseTo(0.12567, offset))
                          .hasEntrySatisfying(1, prob -> assertThat(prob).isCloseTo(0.86652, offset))
                          .containsOnlyKeys(1, 2, 3, 4);
    }

    @Test
    public void orderOfOperationsWithSubtraction() {
        final Expression expression = parser.parseExpression("2 - 1 + 1");

        final Map<Integer, Double> result = calculator.calculateResult(expression)
                                                      .entrySet()
                                                      .stream()
                                                      .collect(toMap(Map.Entry::getKey, e -> e.getValue().getProbability()));

        final Offset<Double> offset = offset(0.00001);
        assertThat(result).hasEntrySatisfying(2, prob -> assertThat(prob).isCloseTo(1.0, offset))
                          .containsOnlyKeys(2);
    }

    @Test
    public void orderOfOperationsWithMultiplication() {
        final Expression expression = parser.parseExpression("2 + 1 * 3");

        final Map<Integer, Double> result = calculator.calculateResult(expression)
                                                      .entrySet()
                                                      .stream()
                                                      .collect(toMap(Map.Entry::getKey, e -> e.getValue().getProbability()));

        final Offset<Double> offset = offset(0.00001);
        assertThat(result).hasEntrySatisfying(5, prob -> assertThat(prob).isCloseTo(1.0, offset))
                          .containsOnlyKeys(5);
    }

    @Test
    public void orderOfOperationsWithDivision() {
        final Expression expression = parser.parseExpression("2 + 1 / 3");

        final Map<Integer, Double> result = calculator.calculateResult(expression)
                                                      .entrySet()
                                                      .stream()
                                                      .collect(toMap(Map.Entry::getKey, e -> e.getValue().getProbability()));

        final Offset<Double> offset = offset(0.00001);
        assertThat(result).hasEntrySatisfying(2, prob -> assertThat(prob).isCloseTo(1.0, offset))
                          .containsOnlyKeys(2);
    }

    @Test
    public void orderOfOperationsWithDivisionAndMultiplication() {
        final Expression expression = parser.parseExpression("3 * 1 / 3");

        final Map<Integer, Double> result = calculator.calculateResult(expression)
                                                      .entrySet()
                                                      .stream()
                                                      .collect(toMap(Map.Entry::getKey, e -> e.getValue().getProbability()));

        final Offset<Double> offset = offset(0.00001);
        assertThat(result).hasEntrySatisfying(0, prob -> assertThat(prob).isCloseTo(1.0, offset))
                          .containsOnlyKeys(0);
    }

    @Test
    public void fullBedmas() {
        final Expression expression = parser.parseExpression("3 * (3 + 1) / 4 * 10");

        final Map<Integer, Double> result = calculator.calculateResult(expression)
                                                      .entrySet()
                                                      .stream()
                                                      .collect(toMap(Map.Entry::getKey, e -> e.getValue().getProbability()));

        final Offset<Double> offset = offset(0.00001);
        assertThat(result).hasEntrySatisfying(30, prob -> assertThat(prob).isCloseTo(1.0, offset))
                          .containsOnlyKeys(30);
    }
}
