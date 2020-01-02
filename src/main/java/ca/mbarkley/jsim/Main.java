package ca.mbarkley.jsim;

import ca.mbarkley.jsim.model.Statement;
import org.antlr.v4.runtime.RecognitionException;

public class Main {
    public static void main(String[] args) {
        final String input;
        if (args.length > 1) {
            input = String.join(" ", args);
        } else {
             throw new IllegalArgumentException("Must supply statement as command line argument");
        }

        final Parser parser = new Parser();
        final int desiredWidth = 120;
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
