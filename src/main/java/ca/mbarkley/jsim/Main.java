package ca.mbarkley.jsim;

import ca.mbarkley.jsim.cli.BatchProcessor;

public class Main {

    public static final int DESIRED_WIDTH = 120;

    public static void main(String[] args) {
        final String input;
        if (args.length > 0) {
            input = String.join(" ", args);
        } else {
             throw new IllegalArgumentException("Must supply statement as command line argument");
        }

        new BatchProcessor(DESIRED_WIDTH).process(input);
    }
}
