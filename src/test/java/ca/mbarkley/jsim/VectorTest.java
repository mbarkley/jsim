package ca.mbarkley.jsim;

import ca.mbarkley.jsim.eval.Evaluation;
import ca.mbarkley.jsim.eval.Parser;
import ca.mbarkley.jsim.model.*;
import ca.mbarkley.jsim.model.Expression.Constant;
import org.junit.Test;

import java.util.Map;

import static ca.mbarkley.jsim.model.Symbol.Mark.TICK;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

public class VectorTest {
    Parser parser = new Parser();

    @Test
    public void addVectorsWithDifferentStartSymbol() {
        final Evaluation eval = parser.parse("{:roll:1} + {'roll:1}");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Vector, Double> result = eval.getExpressions()
                                               .get(0)
                                               .calculateResults()
                                               .entrySet()
                                               .stream()
                                               .collect(toMap(e -> (Vector) e.getKey(), e -> e.getValue().getProbability()));
        assertThat(result.entrySet()).hasSize(1);
        final Vector vector = result.keySet().iterator().next();
        assertThat(vector.getCoordinate()).hasSize(1);
    }

    @Test
    public void symbolVectorEqualsBracesVector() {
        final Evaluation eval = parser.parse("1'H + 2'T = {'H: 1, 'T: 2}");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Boolean, Double> result = eval.getExpressions()
                                                .get(0)
                                                .calculateResults()
                                                .entrySet()
                                                .stream()
                                                .collect(toMap(e -> (Boolean) e.getKey(), e -> e.getValue().getProbability()));
        assertThat(result.keySet()).containsExactlyInAnyOrder(true);
        assertThat(result.values()).containsExactly(1.0);
    }

    @Test
    public void addingSymbolsMakesVector() {
        final Evaluation eval = parser.parse("'H + 'T");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Vector, Double> result = eval.getExpressions()
                                               .get(0)
                                               .calculateResults()
                                               .entrySet()
                                               .stream()
                                               .collect(toMap(e -> (Vector) e.getKey(), e -> e.getValue().getProbability()));
        assertThat(result.keySet()).containsExactlyInAnyOrder(Vectors.of(Map.of(
                new Symbol(TICK, "H"), Constants.of(1),
                new Symbol(TICK, "T"), Constants.of(1)
        )));
        assertThat(result.values()).containsExactly(1.0);
    }

    @Test
    public void addingSymbolAndVectorMakesVector() {
        final Evaluation eval = parser.parse("'H + 1'T");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Vector, Double> result = eval.getExpressions()
                                               .get(0)
                                               .calculateResults()
                                               .entrySet()
                                               .stream()
                                               .collect(toMap(e -> (Vector) e.getKey(), e -> e.getValue().getProbability()));
        assertThat(result.keySet()).containsExactlyInAnyOrder(Vectors.of(Map.of(
                new Symbol(TICK, "H"), Constants.of(1),
                new Symbol(TICK, "T"), Constants.of(1)
        )));
        assertThat(result.values()).containsExactly(1.0);
    }

    @Test
    public void addingVectorAndSymbolMakesVector() {
        final Evaluation eval = parser.parse("1'H + 'T");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Vector, Double> result = eval.getExpressions()
                                               .get(0)
                                               .calculateResults()
                                               .entrySet()
                                               .stream()
                                               .collect(toMap(e -> (Vector) e.getKey(), e -> e.getValue().getProbability()));
        assertThat(result.keySet()).containsExactlyInAnyOrder(Vectors.of(Map.of(
                new Symbol(TICK, "H"), new Constant<>(Types.INTEGER_TYPE, 1),
                new Symbol(TICK, "T"), new Constant<>(Types.INTEGER_TYPE, 1)
        )));
        assertThat(result.values()).containsExactly(1.0);
    }

    @Test
    public void multipleCoinFlipComparison() {
        final Evaluation eval = parser.parse("define coin = ['H, 'T]; coin + coin = 'H + 'T");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Boolean, Double> result = eval.getExpressions()
                                               .get(0)
                                               .calculateResults()
                                               .entrySet()
                                               .stream()
                                               .collect(toMap(e -> (Boolean) e.getKey(), e -> e.getValue().getProbability()));

        assertThat(result.entrySet()).containsExactlyInAnyOrder(
                Map.entry(true, 1.0/2.0),
                Map.entry(false, 1.0/2.0)
        );
    }

    @Test
    public void multipleCoinFlipComparisonWithSingleSymbol() {
        final Evaluation eval = parser.parse("define coin = ['H, 'T]; coin + coin = 2'H");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Boolean, Double> result = eval.getExpressions()
                                                .get(0)
                                                .calculateResults()
                                                .entrySet()
                                                .stream()
                                                .collect(toMap(e -> (Boolean) e.getKey(), e -> e.getValue().getProbability()));

        assertThat(result.entrySet()).containsExactlyInAnyOrder(
                Map.entry(true, 1.0/4.0),
                Map.entry(false, 3.0/4.0)
        );
    }

    @Test
    public void propertyAccessTest() {
        final Evaluation eval = parser.parse("define test = [{'v: 1}, {'v: 2}]; test{'v}");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Integer, Double> result = eval.getExpressions()
                                                .get(0)
                                                .calculateResults()
                                                .entrySet()
                                                .stream()
                                                .collect(toMap(e -> (Integer) e.getKey(), e -> e.getValue().getProbability()));
        assertThat(result.keySet()).containsExactlyInAnyOrder(1, 2);
        assertThat(result.values()).allSatisfy(prob -> assertThat(prob).isCloseTo(0.5, offset(0.0001)));
    }
}
