package ca.mbarkley.jsim.cli;

import ca.mbarkley.jsim.Displayer;
import ca.mbarkley.jsim.Parser;
import ca.mbarkley.jsim.model.Statement;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.RecognitionException;

import java.io.Console;

public class ConsoleProcessor {
    private final Parser parser;
    private final Displayer displayer;

    public ConsoleProcessor(int desiredWidth) {
        parser = new Parser();
        displayer = new Displayer(desiredWidth);
    }


    public void process(Console console) {
        do {
            console.printf("$ ");
            final String line = console.readLine();
            if (line == null) {
                break;
            } else {
                try {
                    final Statement<?> stmt = parser.parse(line);
                    final String sortedHistogram = displayer.createSortedHistogram(stmt.toString(), stmt.events());
                    console.printf("%s", sortedHistogram);
                } catch (RecognitionException re) {
                    console.printf("Invalid symbol: line %d, position %d\n", re.getOffendingToken().getLine(), re.getOffendingToken().getCharPositionInLine());
                } catch (RuntimeException re) {
                    console.printf("Problem while evaluating statement: %s\n", re.getMessage());
                }
            }
        } while (true);
    }
}
