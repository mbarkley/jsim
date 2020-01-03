package ca.mbarkley.jsim;

import ca.mbarkley.jsim.cli.BatchProcessor;
import ca.mbarkley.jsim.cli.ConsoleProcessor;
import org.apache.commons.cli.*;

import java.io.Console;

public class Main {

    public static final int DESIRED_WIDTH = 120;

    public static void main(String[] args) {
        try {
            final CommandLine commandLine = parseCommandLine(args);

            if (commandLine.hasOption("c")) {
                final String input = String.join(" ", commandLine.getArgs());
                if (input.isEmpty()) {
                    System.err.println("No script specified with '-c' flag");
                }

                new BatchProcessor(DESIRED_WIDTH).process(input);
            } else {
                final Console console = System.console();
                if (console != null) {
                    new ConsoleProcessor(DESIRED_WIDTH).process(console);
                } else {
                    new BatchProcessor(DESIRED_WIDTH).process(System.in);
                }
            }

        } catch (ParseException e) {
            System.err.printf("Invalid arguments: %s\n", e.getMessage());
            System.exit(1);
        }

    }

    private static CommandLine parseCommandLine(String[] args) throws ParseException {
        final Options options = options();
        final CommandLineParser argParser = new DefaultParser();
        return argParser.parse(options, args, true);
    }

    private static Options options() {
        final Options options = new Options();
        final OptionGroup inputGroup = new OptionGroup();
        inputGroup.addOption(Option.builder("c")
                                   .argName("command string")
                                   .desc("run script from first non-option command arguments")
                                   .required()
                                   .build());

        options.addOptionGroup(inputGroup);
        return options;
    }
}
