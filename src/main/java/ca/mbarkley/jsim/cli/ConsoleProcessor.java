package ca.mbarkley.jsim.cli;

import ca.mbarkley.jsim.eval.*;
import ca.mbarkley.jsim.model.Expression;
import org.antlr.v4.runtime.RecognitionException;

import java.io.Console;
import java.util.HashMap;
import java.util.Map;

public class ConsoleProcessor {
    private final Parser parser;
    private final Displayer displayer;

    public ConsoleProcessor(int desiredWidth) {
        parser = new Parser();
        displayer = new Displayer(desiredWidth);
    }


    public void process(Console console) {
        final Map<String, Expression<?>> definitions = new HashMap<>();
        do {
            console.printf("$ ");
            final String line = console.readLine();
            if (line == null) {
                break;
            } else {
                try {
                    final Evaluation eval = parser.parse(new Context(definitions), line);
                    definitions.putAll(eval.getContext().getDefinitions());
                    for (var expression : eval.getExpressions()) {
                        final String sortedHistogram = displayer.createSortedHistogram(expression.toString(), expression.events());
                        console.printf("%s", sortedHistogram);
                    }
                } catch (RecognitionException re) {
                    console.printf("Invalid symbol: line %d, position %d\n", re.getOffendingToken().getLine(), re.getOffendingToken().getCharPositionInLine());
                } catch (EvaluationException.UndefinedIdentifierException uie) {
                    console.printf("%s\n", uie.getMessage());
                } catch (RuntimeException re) {
                    console.printf("Problem while evaluating statement: %s\n", re.getMessage());
                }
            }
        } while (true);
    }
}
