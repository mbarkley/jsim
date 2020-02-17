package ca.mbarkley.jsim;

import ca.mbarkley.jsim.eval.Evaluation;
import ca.mbarkley.jsim.eval.Parser;
import ca.mbarkley.jsim.model.Expression;
import ca.mbarkley.jsim.model.Type;
import ca.mbarkley.jsim.model.Vector;
import ca.mbarkley.jsim.prob.Event;
import org.assertj.core.data.Offset;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static ca.mbarkley.jsim.model.Types.INTEGER_TYPE;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

public class CalculatorTest {
    Parser parser = new Parser();

    @Test
    public void rollLessThanConstant() {
        final List<Expression<?>> stmts = parser.parse("d6 < 4").getExpressions();

        final Map<?, ? extends Event<?>> result = stmts.get(0).calculateResults();

        Assert.assertEquals(0.5, result.get(true).getProbability(), 0.0001);
    }

    @Test
    public void complexRollGreaterThanConstant() {
        final List<Expression<?>> stmts = parser.parse("2d6 + 1 > 6").getExpressions();

        final Map<?, ? extends Event<?>> result = stmts.get(0).calculateResults();

        Assert.assertEquals(0.722, result.get(true).getProbability(), 0.001);
    }

    @Test
    public void multipliedRollsGreaterThanConstant() {
        final List<Expression<?>> stmts = parser.parse("d4 * d4 > 8").getExpressions();

        final Map<?, ? extends Event<?>> result = stmts.get(0).calculateResults();

        Assert.assertEquals(0.250, result.get(true).getProbability(), 0.001);
    }

    @Test
    public void complexRollLessThanComplexRoll() {
        final List<Expression<?>> stmts = parser.parse("2d6 + 1 < 2d8 - 4").getExpressions();

        final Map<?, ? extends Event<?>> result = stmts.get(0).calculateResults();

        Assert.assertEquals(0.20095, result.get(true).getProbability(), 0.00001);
    }

    @Test
    public void constantLessThanConstant() {
        final List<Expression<?>> stmts = parser.parse("1 < 2").getExpressions();

        final Map<?, ? extends Event<?>> result = stmts.get(0).calculateResults();

        Assert.assertEquals(1.0, result.get(true).getProbability(), 0.00001);
    }

    @Test
    public void bigAdditionQuestion() {
        final List<Expression<?>> stmts = parser.parse("6d20 + 14d20 > 200").getExpressions();

        final Map<?, ? extends Event<?>> result = stmts.get(0).calculateResults();

        Assert.assertEquals(0.643, result.get(true).getProbability(), 0.001);
    }

    @Test
    public void bigMultiRollQuestion() {
        final List<Expression<?>> stmts = parser.parse("20d20 > 200").getExpressions();

        final Map<?, ? extends Event<?>> result = stmts.get(0).calculateResults();

        Assert.assertEquals(0.643, result.get(true).getProbability(), 0.001);
    }

    @Test
    public void reallyBigMultiRollQuestion() {
        final List<Expression<?>> stmts = parser.parse("100d20 > 1000").getExpressions();

        final Map<?, ? extends Event<?>> result = stmts.get(0).calculateResults();

        Assert.assertEquals(0.804, result.get(true).getProbability(), 0.001);
    }

    @Test
    public void simpleExpressionResults() {
        final List<Expression<?>> stmts = parser.parse("2d4").getExpressions();

        final Map<Integer, Double> result = stmts.get(0)
                                                 .calculateResults()
                                                 .entrySet()
                                                 .stream()
                                                 .collect(toMap(e -> (Integer) e.getKey(), e -> e.getValue().getProbability()));

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
        final List<Expression<?>> stmts = parser.parse("7d4H1").getExpressions();

        final Map<Integer, Double> result = stmts.get(0)
                                                 .calculateResults()
                                                 .entrySet()
                                                 .stream()
                                                 .collect(toMap(e -> (Integer) e.getKey(), e -> e.getValue().getProbability()));

        final Offset<Double> offset = offset(0.00001);
        assertThat(result).hasEntrySatisfying(1, prob -> assertThat(prob).isCloseTo(0.00006, offset))
                          .hasEntrySatisfying(2, prob -> assertThat(prob).isCloseTo(0.00775, offset))
                          .hasEntrySatisfying(3, prob -> assertThat(prob).isCloseTo(0.12567, offset))
                          .hasEntrySatisfying(4, prob -> assertThat(prob).isCloseTo(0.86652, offset))
                          .containsOnlyKeys(1, 2, 3, 4);
    }

    @Test
    public void lowDiceExpressionResults() {
        final List<Expression<?>> stmts = parser.parse("7d4L1").getExpressions();

        final Map<Integer, Double> result = stmts.get(0)
                                                 .calculateResults()
                                                 .entrySet()
                                                 .stream()
                                                 .collect(toMap(e -> (Integer) e.getKey(), e -> e.getValue().getProbability()));

        final Offset<Double> offset = offset(0.00001);
        assertThat(result).hasEntrySatisfying(4, prob -> assertThat(prob).isCloseTo(0.00006, offset))
                          .hasEntrySatisfying(3, prob -> assertThat(prob).isCloseTo(0.00775, offset))
                          .hasEntrySatisfying(2, prob -> assertThat(prob).isCloseTo(0.12567, offset))
                          .hasEntrySatisfying(1, prob -> assertThat(prob).isCloseTo(0.86652, offset))
                          .containsOnlyKeys(1, 2, 3, 4);
    }

    @Test
    public void orderOfOperationsWithSubtraction() {
        final List<Expression<?>> stmts = parser.parse("2 - 1 + 1").getExpressions();

        final Map<Integer, Double> result = stmts.get(0)
                                                 .calculateResults()
                                                 .entrySet()
                                                 .stream()
                                                 .collect(toMap(e -> (Integer) e.getKey(), e -> e.getValue().getProbability()));

        final Offset<Double> offset = offset(0.00001);
        assertThat(result).hasEntrySatisfying(2, prob -> assertThat(prob).isCloseTo(1.0, offset))
                          .containsOnlyKeys(2);
    }

    @Test
    public void orderOfOperationsWithMultiplication() {
        final List<Expression<?>> stmts = parser.parse("2 + 1 * 3").getExpressions();

        final Map<Integer, Double> result = stmts.get(0)
                                                 .calculateResults()
                                                 .entrySet()
                                                 .stream()
                                                 .collect(toMap(e -> (Integer) e.getKey(), e -> e.getValue().getProbability()));

        final Offset<Double> offset = offset(0.00001);
        assertThat(result).hasEntrySatisfying(5, prob -> assertThat(prob).isCloseTo(1.0, offset))
                          .containsOnlyKeys(5);
    }

    @Test
    public void orderOfOperationsWithDivision() {
        final List<Expression<?>> stmts = parser.parse("2 + 1 / 3").getExpressions();

        final Map<Integer, Double> result = stmts.get(0)
                                                 .calculateResults()
                                                      .entrySet()
                                                      .stream()
                                                      .collect(toMap(e -> (Integer) e.getKey(), e -> e.getValue().getProbability()));

        final Offset<Double> offset = offset(0.00001);
        assertThat(result).hasEntrySatisfying(2, prob -> assertThat(prob).isCloseTo(1.0, offset))
                          .containsOnlyKeys(2);
    }

    @Test
    public void orderOfOperationsWithDivisionAndMultiplication() {
        final List<Expression<?>> stmts = parser.parse("3 * 1 / 3").getExpressions();

        final Map<Integer, Double> result = stmts.get(0)
                                                 .calculateResults()
                                                      .entrySet()
                                                      .stream()
                                                      .collect(toMap(e -> (Integer) e.getKey(), e -> e.getValue().getProbability()));

        final Offset<Double> offset = offset(0.00001);
        assertThat(result).hasEntrySatisfying(0, prob -> assertThat(prob).isCloseTo(1.0, offset))
                          .containsOnlyKeys(0);
    }

    @Test
    public void fullBedmas() {
        final List<Expression<?>> stmts = parser.parse("3 * (3 + 1) / 4 * 10").getExpressions();

        final Map<Integer, Double> result = stmts.get(0)
                                                 .calculateResults()
                                                 .entrySet()
                                                 .stream()
                                                 .collect(toMap(e -> (Integer) e.getKey(), e -> e.getValue().getProbability()));

        final Offset<Double> offset = offset(0.00001);
        assertThat(result).hasEntrySatisfying(30, prob -> assertThat(prob).isCloseTo(1.0, offset))
                          .containsOnlyKeys(30);
    }

    @Test
    public void disjunctive() {
        final List<Expression<?>> stmts = parser.parse("d6 = 1 or d6 = 2").getExpressions();

        final Map<?, ? extends Event<?>> result = stmts.get(0).calculateResults();

        Assert.assertEquals(11.0 / 36.0, result.get(true).getProbability(), 0.0001);
    }

    @Test
    public void conjunctive() {
        final List<Expression<?>> stmts = parser.parse("d6 = 1 and d6 = 2").getExpressions();

        final Map<?, ? extends Event<?>> result = stmts.get(0).calculateResults();

        Assert.assertEquals(1.0 / 36.0, result.get(true).getProbability(), 0.0001);
    }

    @Test
    public void booleanOrderOfOperations() {
        final List<Expression<?>> stmts = parser.parse("d6 = 1 and d6 = 2 or d100 = 1").getExpressions();

        final Map<?, ? extends Event<?>> result = stmts.get(0).calculateResults();

        Assert.assertEquals(1.0 - ((35.0 / 36.0) * (99.0 / 100.0)), result.get(true).getProbability(), 0.0001);
    }

    @Test
    public void booleanOrderOfOperationsWithBracketing() {
        final List<Expression<?>> stmts = parser.parse("d6 = 1 and (d6 = 2 or d100 = 1)").getExpressions();

        final Map<?, ? extends Event<?>> result = stmts.get(0).calculateResults();

        Assert.assertEquals(1.0/6.0 * (1.0 - (5.0/6.0 * 99.0/100.0)), result.get(true).getProbability(), 0.0001);
    }

    @Test
    public void booleanConstantExpression() {
        final  List<Expression<?>> stmts = parser.parse("true").getExpressions();

        final Map<?, ? extends Event<?>> result = stmts.get(0).calculateResults();

        Assert.assertEquals(1.0, result.get(true).getProbability(), 0.0001);
    }

    @Test
    public void booleanEqualityExpression() {
        final  List<Expression<?>> stmts = parser.parse("true = true").getExpressions();

        final Map<?, ? extends Event<?>> result = stmts.get(0).calculateResults();

        Assert.assertEquals(1.0, result.get(true).getProbability(), 0.0001);
    }

    @Test
    public void symbolicEqualityExpression() {
        final  List<Expression<?>> stmts = parser.parse("'foo = 'foo").getExpressions();

        final Map<?, ? extends Event<?>> result = stmts.get(0).calculateResults();

        Assert.assertEquals(1.0, result.get(true).getProbability(), 0.0001);
    }

    @Test
    public void simpleLetExpression() {
        // Want to do modulo operator for this use-case but isn't implemented yet
        final List<Expression<?>> stmts = parser.parse("let x <- 1d4 in x > 2").getExpressions();

        final Map<Boolean, Double> result = stmts.get(0)
                                                 .calculateResults()
                                                 .entrySet()
                                                 .stream()
                                                 .collect(toMap(e -> (Boolean) e.getKey(), e -> e.getValue().getProbability()));

        final Offset<Double> offset = offset(0.00001);
        assertThat(result).hasEntrySatisfying(true, prob -> assertThat(prob).isCloseTo(1.0/2.0, offset))
                          .hasEntrySatisfying(false, prob -> assertThat(prob).isCloseTo(1.0/2.0, offset));
    }

    @Test
    public void complexLetExpression() {
        // Want to do modulo operator for this use-case but isn't implemented yet
        final List<Expression<?>> stmts = parser.parse("let x <- 2d6 in x > 10 and x / 2 * 2 = x").getExpressions();

        final Map<Boolean, Double> result = stmts.get(0)
                                                 .calculateResults()
                                                 .entrySet()
                                                 .stream()
                                                 .collect(toMap(e -> (Boolean) e.getKey(), e -> e.getValue().getProbability()));

        final Offset<Double> offset = offset(0.00001);
        assertThat(result).hasEntrySatisfying(true, prob -> assertThat(prob).isCloseTo(1.0/36.0, offset))
                          .hasEntrySatisfying(false, prob -> assertThat(prob).isCloseTo(35.0/36.0, offset));
    }
}
