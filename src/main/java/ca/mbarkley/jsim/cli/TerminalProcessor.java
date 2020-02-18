package ca.mbarkley.jsim.cli;

import ca.mbarkley.jsim.eval.Evaluation;
import ca.mbarkley.jsim.eval.EvaluationException;
import ca.mbarkley.jsim.eval.LexicalScope;
import ca.mbarkley.jsim.eval.Parser;
import ca.mbarkley.jsim.model.Expression;
import org.antlr.v4.runtime.RecognitionException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class TerminalProcessor {
    private final Parser parser;

    public TerminalProcessor() {
        parser = new Parser();
    }


    public void process(Terminal terminal) {
        final Map<String, Expression<?>> definitions = new HashMap<>();
        final LineReader lineReader = LineReaderBuilder.builder()
                                                       .terminal(terminal)
                                                       .appName(terminal.getName())
                                                       .build();
        final Displayer displayer = new Displayer(terminal::getWidth);
        final PrintWriter writer = terminal.writer();
        try {
            do {
                final String line = lineReader.readLine("$ ");
                if (line == null) {
                    break;
                } else {
                    try {
                        final Evaluation eval = parser.parse(new LexicalScope(definitions), line);
                        definitions.putAll(eval.getContext().getDefinitions());
                        for (var expression : eval.getExpressions()) {
                            final String sortedHistogram = displayer.createSortedHistogram(expression.toString(), expression.calculateResults()
                                                                                                                            .values()
                                                                                                                            .stream());
                            writer.printf("%s", sortedHistogram);
                        }
                    } catch (RecognitionException re) {
                        writer.printf("Invalid symbol: line %d, position %d\n", re.getOffendingToken().getLine(), re.getOffendingToken().getCharPositionInLine());
                    } catch (EvaluationException.UndefinedIdentifierException uie) {
                        writer.printf("%s\n", uie.getMessage());
                    } catch (RuntimeException re) {
                        writer.printf("Problem while evaluating statement: %s\n", re.getMessage());
                    }
                }
            } while (true);
        } catch (UserInterruptException uie) {
            // Exit
        }
    }
}
