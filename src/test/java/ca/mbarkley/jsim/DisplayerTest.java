package ca.mbarkley.jsim;

import ca.mbarkley.jsim.cli.Displayer;
import ca.mbarkley.jsim.eval.Parser;
import ca.mbarkley.jsim.model.Expression;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DisplayerTest {

    Parser parser = new Parser();
    Displayer displayer = new Displayer(() -> 120);

    @Test
    public void simpleHistogram() {
        final List<Expression<?>> stmts = parser.parse("2d6").getExpressions();
        final String sortedHistogram = displayer.createSortedHistogram("2d6", stmts.get(0).calculateResults().values().stream());
        assertThat(sortedHistogram).isEqualTo(
                "" +
                        "--------------------------------------------------------- 2d6  ---------------------------------------------------------\n" +
                        "2  |******************                                                                                             2.78%\n" +
                        "3  |************************************                                                                           5.56%\n" +
                        "4  |******************************************************                                                         8.33%\n" +
                        "5  |************************************************************************                                      11.11%\n" +
                        "6  |******************************************************************************************                    13.89%\n" +
                        "7  |************************************************************************************************************  16.67%\n" +
                        "8  |******************************************************************************************                    13.89%\n" +
                        "9  |************************************************************************                                      11.11%\n" +
                        "10 |******************************************************                                                         8.33%\n" +
                        "11 |************************************                                                                           5.56%\n" +
                        "12 |******************                                                                                             2.78%\n"
        );
    }
}