package ca.mbarkley.jsim;

import ca.mbarkley.jsim.eval.Evaluation;
import ca.mbarkley.jsim.eval.Parser;
import ca.mbarkley.jsim.model.Expression;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
}
