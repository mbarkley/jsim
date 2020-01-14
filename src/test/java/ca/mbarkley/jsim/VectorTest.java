package ca.mbarkley.jsim;

import ca.mbarkley.jsim.eval.Evaluation;
import ca.mbarkley.jsim.eval.EvaluationException;
import ca.mbarkley.jsim.eval.EvaluationException.DiceTypeException;
import ca.mbarkley.jsim.eval.Parser;
import ca.mbarkley.jsim.model.Expression;
import ca.mbarkley.jsim.model.Expression.Constant;
import ca.mbarkley.jsim.model.Type;
import ca.mbarkley.jsim.model.Vector;
import org.assertj.core.data.Offset;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static ca.mbarkley.jsim.model.Types.INTEGER_TYPE;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
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
}
