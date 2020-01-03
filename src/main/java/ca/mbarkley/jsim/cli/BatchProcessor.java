package ca.mbarkley.jsim.cli;

import ca.mbarkley.jsim.Displayer;
import ca.mbarkley.jsim.Parser;
import ca.mbarkley.jsim.model.Statement;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.RecognitionException;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class BatchProcessor {
    private final int desiredWidth;
    private final Parser parser;
    private final Displayer displayer;

    public BatchProcessor(int desiredWidth) {
        this.desiredWidth = desiredWidth;
        parser = new Parser();
        displayer = new Displayer(desiredWidth);
    }

    public void process(String input) {
        try {
            final Statement<?> stmt = parser.parse(input);
            final String cleanInput = stmt.toString();
            final String sortedHistogram = displayer.createSortedHistogram(cleanInput, stmt.events());

            System.out.print(sortedHistogram);
        } catch (RecognitionException re) {
            System.err.printf("Invalid symbol: line %d, position %d\n", re.getOffendingToken().getLine(), re.getOffendingToken().getCharPositionInLine());
            System.exit(1);
        }
    }

    public void process(InputStream in) {
        try {
            process(IOUtils.toString(in));
        } catch (IOException e) {
            System.err.printf("Error reading input: %s\n", e.getMessage());
            System.exit(1);
        }
    }
}
