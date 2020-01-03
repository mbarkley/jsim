package ca.mbarkley.jsim.cli;

import ca.mbarkley.jsim.Displayer;
import ca.mbarkley.jsim.Parser;
import ca.mbarkley.jsim.model.Statement;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.RecognitionException;

@RequiredArgsConstructor
public class BatchProcessor {
    private final int desiredWidth;

    public void process(String input) {
        final Parser parser = new Parser();
        final Displayer displayer = new Displayer(desiredWidth);
        try {
            final Statement<?> stmt = parser.parse(input);
            final String sortedHistogram = displayer.createSortedHistogram(stmt.events());

            final int barLength = (desiredWidth - input.length() - 2) / 2;
            final int remainderAdjustment = (desiredWidth - input.length() - 2) % 2;
            System.out.printf("%s %s %s\n", "-".repeat(barLength), input, " ".repeat(remainderAdjustment) + "-".repeat(barLength));
            System.out.print(sortedHistogram);
        } catch (RecognitionException re) {
            System.err.printf("Invalid symbol: line %d, position %d\n", re.getOffendingToken().getLine(), re.getOffendingToken().getCharPositionInLine());
            System.exit(1);
        }
    }
}
