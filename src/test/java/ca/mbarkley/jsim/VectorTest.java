package ca.mbarkley.jsim;

import ca.mbarkley.jsim.eval.Evaluation;
import ca.mbarkley.jsim.eval.Parser;
import ca.mbarkley.jsim.model.*;
import ca.mbarkley.jsim.model.Expression.Constant;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

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
}
