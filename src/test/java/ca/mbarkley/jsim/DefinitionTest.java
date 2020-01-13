package ca.mbarkley.jsim;

import ca.mbarkley.jsim.eval.Evaluation;
import ca.mbarkley.jsim.eval.EvaluationException;
import ca.mbarkley.jsim.eval.EvaluationException.DiceTypeException;
import ca.mbarkley.jsim.eval.Parser;
import ca.mbarkley.jsim.model.Expression;
import ca.mbarkley.jsim.model.Expression.Constant;
import ca.mbarkley.jsim.model.Type;
import ca.mbarkley.jsim.model.Vector;
import ca.mbarkley.jsim.model.Vector.Dimension;
import org.assertj.core.data.Offset;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static ca.mbarkley.jsim.model.Types.INTEGER_TYPE;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.data.Offset.offset;

public class DefinitionTest {
    Parser parser = new Parser();

    @Test
    public void definitionYieldsNoExpressions() {
        final List<Expression<?>> stmts = parser.parse("define myRoll = 2d6 + 1").getExpressions();

        assertThat(stmts).isEmpty();
    }

    @Test
    public void definitionInContext() {
        final Evaluation eval = parser.parse("define myRoll = 2d6 + 1");

        assertThat(eval.getContext()
                       .getDefinitions()).containsOnlyKeys("myRoll")
                                         .hasEntrySatisfying("myRoll", exp -> assertThat(exp).hasToString("2d6 + 1"));
    }

    @Test
    public void evaluateIntegerDefinition() {
        final Evaluation eval = parser.parse("define myRoll = d6 + 1; myRoll > 6");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Boolean, Double> result = eval.getExpressions()
                                                .get(0)
                                                .calculateResults()
                                                .entrySet()
                                                .stream()
                                                .collect(toMap(e -> (Boolean) e.getKey(), e -> e.getValue().getProbability()));
        final Offset<Double> offset = offset(0.0001);
        assertThat(result).hasEntrySatisfying(true, prob -> assertThat(prob).isCloseTo(1.0 / 6.0, offset))
                          .hasEntrySatisfying(false, prob -> assertThat(prob).isCloseTo(5.0 / 6.0, offset))
                          .containsOnlyKeys(true, false);
    }

    @Test
    public void evaluateBooleanDefinition() {
        final Evaluation eval = parser.parse("define myTest = d6 > 3; myTest and true");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Boolean, Double> result = eval.getExpressions()
                                                .get(0)
                                                .calculateResults()
                                                .entrySet()
                                                .stream()
                                                .collect(toMap(e -> (Boolean) e.getKey(), e -> e.getValue().getProbability()));
        final Offset<Double> offset = offset(0.0001);
        assertThat(result).hasEntrySatisfying(true, prob -> assertThat(prob).isCloseTo(0.5, offset))
                          .hasEntrySatisfying(false, prob -> assertThat(prob).isCloseTo(0.5, offset))
                          .containsOnlyKeys(true, false);
    }

    @Test
    public void evaluateBooleanWithAmbiguousCallSite() {
        final Evaluation eval = parser.parse("define myTest = d6 > 3; myTest");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Boolean, Double> result = eval.getExpressions()
                                                .get(0)
                                                .calculateResults()
                                                .entrySet()
                                                .stream()
                                                .collect(toMap(e -> (Boolean) e.getKey(), e -> e.getValue().getProbability()));
        final Offset<Double> offset = offset(0.0001);
        assertThat(result).hasEntrySatisfying(true, prob -> assertThat(prob).isCloseTo(0.5, offset))
                          .hasEntrySatisfying(false, prob -> assertThat(prob).isCloseTo(0.5, offset))
                          .containsOnlyKeys(true, false);
    }

    @Test
    public void undefinedUsage() {
        try {
            final Evaluation eval = parser.parse("missingVar");
            fail("Should not succeed");
        } catch (EvaluationException.UndefinedIdentifierException uie) {
            assertThat(uie.getMessage()).contains("missingVar");
        } catch (RuntimeException re) {
            throw new AssertionError("Wrong exception type", re);
        }
    }

    @Test
    public void evaluateCustomIntegerDiceDefinition() {
        final Evaluation eval = parser.parse("define myTest = [1, 3, 5]; myTest + 1");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Integer, Double> result = eval.getExpressions()
                                                .get(0)
                                                .calculateResults()
                                                .entrySet()
                                                .stream()
                                                .collect(toMap(e -> (Integer) e.getKey(), e -> e.getValue().getProbability()));
        final Offset<Double> offset = offset(0.0001);
        assertThat(result).hasEntrySatisfying(2, prob -> assertThat(prob).isCloseTo(1.0/3.0, offset))
                          .hasEntrySatisfying(4, prob -> assertThat(prob).isCloseTo(1.0/3.0, offset))
                          .hasEntrySatisfying(6, prob -> assertThat(prob).isCloseTo(1.0/3.0, offset))
                          .containsOnlyKeys(2, 4, 6);
    }

    @Test
    public void evaluateCustomBooleanDiceDefinition() {
        final Evaluation eval = parser.parse("define myTest = [true, true, false]; myTest");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Boolean, Double> result = eval.getExpressions()
                                                .get(0)
                                                .calculateResults()
                                                .entrySet()
                                                .stream()
                                                .collect(toMap(e -> (Boolean) e.getKey(), e -> e.getValue().getProbability()));
        final Offset<Double> offset = offset(0.0001);
        assertThat(result).hasEntrySatisfying(true, prob -> assertThat(prob).isCloseTo(2.0/3.0, offset))
                          .hasEntrySatisfying(false, prob -> assertThat(prob).isCloseTo(1.0/3.0, offset))
                          .containsOnlyKeys(true, false);
    }

    @Test
    public void evaluateCustomSymbolDiceDefinition() {
        final Evaluation eval = parser.parse("define coin = ['heads, 'tails]; coin = 'heads");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Boolean, Double> result = eval.getExpressions()
                                                .get(0)
                                                .calculateResults()
                                                .entrySet()
                                                .stream()
                                                .collect(toMap(e -> (Boolean) e.getKey(), e -> e.getValue().getProbability()));
        final Offset<Double> offset = offset(0.0001);
        assertThat(result).hasEntrySatisfying(true, prob -> assertThat(prob).isCloseTo(1.0/2.0, offset))
                          .hasEntrySatisfying(false, prob -> assertThat(prob).isCloseTo(1.0/2.0, offset))
                          .containsOnlyKeys(true, false);
    }

    @Test
    public void evaluateConstantSymbolDefinition() {
        final Evaluation eval = parser.parse("define heads = 'heads; heads = 'heads");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Boolean, Double> result = eval.getExpressions()
                                                .get(0)
                                                .calculateResults()
                                                .entrySet()
                                                .stream()
                                                .collect(toMap(e -> (Boolean) e.getKey(), e -> e.getValue().getProbability()));
        final Offset<Double> offset = offset(0.0001);
        assertThat(result).hasEntrySatisfying(true, prob -> assertThat(prob).isCloseTo(1.0, offset));
    }

    @Test
    public void evaluateConstantVectorDefinition() {
        final Evaluation eval = parser.parse("define three = 3; define vector = { 'one : 1, 'two : 2, 'three : three }; vector");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Vector, Double> result = eval.getExpressions()
                                               .get(0)
                                               .calculateResults()
                                               .entrySet()
                                               .stream()
                                               .collect(toMap(e -> (Vector) e.getKey(), e -> e.getValue().getProbability()));
        final Offset<Double> offset = offset(0.0001);
        final Type.VectorType type = new Type.VectorType(new TreeMap<>(Map.of("'one", INTEGER_TYPE, "'two", INTEGER_TYPE, "'three", INTEGER_TYPE)));
        final List<Dimension<?>> coordinate = List.of(
                new Dimension<>( "'one", new Constant<>(INTEGER_TYPE, 1)),
                new Dimension<>("'two", new Constant<>(INTEGER_TYPE, 2)),
                new Dimension<>("'three", new Constant<>(INTEGER_TYPE, 3))
        );
        final Vector vector = new Vector(type, coordinate);
        assertThat(result).hasEntrySatisfying(vector, prob -> assertThat(prob).isCloseTo(1.0, offset));
    }

    @Test
    public void defineAndUseVectorDie() {
        final Evaluation eval = parser.parse("define attack = [{'dmg:1, 'range:2}, {'dmg:2,'range:1}]; attack");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Vector, Double> result = eval.getExpressions()
                                               .get(0)
                                               .calculateResults()
                                               .entrySet()
                                               .stream()
                                               .collect(toMap(e -> (Vector) e.getKey(), e -> e.getValue().getProbability()));
        final Offset<Double> offset = offset(0.0001);
        final Type.VectorType type = new Type.VectorType(new TreeMap<>(Map.of("'dmg", INTEGER_TYPE, "'range", INTEGER_TYPE)));
        final List<Dimension<?>> coordinate1 = List.of(
                new Dimension<>( "'dmg", new Constant<>(INTEGER_TYPE, 1)),
                new Dimension<>("'range", new Constant<>(INTEGER_TYPE, 2))
        );
        final List<Dimension<?>> coordinate2 = List.of(
                new Dimension<>( "'dmg", new Constant<>(INTEGER_TYPE, 2)),
                new Dimension<>("'range", new Constant<>(INTEGER_TYPE, 1))
        );
        final Vector vector1 = new Vector(type, coordinate1);
        final Vector vector2 = new Vector(type, coordinate2);
        assertThat(result).hasEntrySatisfying(vector1, prob -> assertThat(prob).isCloseTo(0.5, offset))
                          .hasEntrySatisfying(vector2, prob -> assertThat(prob).isCloseTo(0.5, offset))
                          .containsOnlyKeys(vector1, vector2);
    }

    @Test
    public void defineAndUseVectorDieWithImpliedSupertype() {
        final Evaluation eval = parser.parse("define attack = [{'dmg:1}, {'dmg:1,'range:1}]; attack");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Vector, Double> result = eval.getExpressions()
                                               .get(0)
                                               .calculateResults()
                                               .entrySet()
                                               .stream()
                                               .collect(toMap(e -> (Vector) e.getKey(), e -> e.getValue().getProbability()));
        final Offset<Double> offset = offset(0.0001);
        final Type.VectorType type = new Type.VectorType(new TreeMap<>(Map.of("'dmg", INTEGER_TYPE, "'range", INTEGER_TYPE)));
        final List<Dimension<?>> coordinate1 = List.of(
                new Dimension<>( "'dmg", new Constant<>(INTEGER_TYPE, 1)),
                new Dimension<>("'range", new Constant<>(INTEGER_TYPE, 0))
        );
        final List<Dimension<?>> coordinate2 = List.of(
                new Dimension<>( "'dmg", new Constant<>(INTEGER_TYPE, 1)),
                new Dimension<>("'range", new Constant<>(INTEGER_TYPE, 1))
        );
        final Vector vector1 = new Vector(type, coordinate1);
        final Vector vector2 = new Vector(type, coordinate2);
        assertThat(result).hasEntrySatisfying(vector1, prob -> assertThat(prob).isCloseTo(0.5, offset))
                          .hasEntrySatisfying(vector2, prob -> assertThat(prob).isCloseTo(0.5, offset))
                          .containsOnlyKeys(vector1, vector2);
    }

    @Test
    public void addVectors() {
        final Evaluation eval = parser.parse("define roll = [{'roll:1}, {'roll:2}]; roll + roll");

        assertThat(eval.getExpressions()).hasSize(1);
        final Map<Vector, Double> result = eval.getExpressions()
                                               .get(0)
                                               .calculateResults()
                                               .entrySet()
                                               .stream()
                                               .collect(toMap(e -> (Vector) e.getKey(), e -> e.getValue().getProbability()));
        final Offset<Double> offset = offset(0.0001);
        final Type.VectorType type = new Type.VectorType(new TreeMap<>(Map.of("'roll", INTEGER_TYPE)));
        final List<Vector.Dimension<?>> coordinate1 = List.of(
                new Vector.Dimension<>("'roll", new Expression.Constant<>(INTEGER_TYPE, 2))
        );
        final List<Vector.Dimension<?>> coordinate2 = List.of(
                new Vector.Dimension<>("'roll", new Expression.Constant<>(INTEGER_TYPE, 3))
        );
        final List<Vector.Dimension<?>> coordinate3 = List.of(
                new Vector.Dimension<>("'roll", new Expression.Constant<>(INTEGER_TYPE, 4))
        );
        final Vector vector1 = new Vector(type, coordinate1);
        final Vector vector2 = new Vector(type, coordinate2);
        final Vector vector3 = new Vector(type, coordinate3);
        assertThat(result).hasEntrySatisfying(vector1, prob -> assertThat(prob).isCloseTo(0.25, offset))
                          .hasEntrySatisfying(vector2, prob -> assertThat(prob).isCloseTo(0.5, offset))
                          .hasEntrySatisfying(vector2, prob -> assertThat(prob).isCloseTo(0.25, offset))
                          .containsOnlyKeys(vector1, vector2, vector3);
    }

    @Test(expected = DiceTypeException.class)
    public void mismatchedDiceType() {
        parser.parse("define myTest = [1, 3, true]; myTest + 1");
    }
}
