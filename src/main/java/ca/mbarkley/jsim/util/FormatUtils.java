package ca.mbarkley.jsim.util;

import static java.lang.String.format;

public class FormatUtils {
    public static String formatAsPercentage(double probability) {
        return format("%.2f%%", 100.0 * probability);
    }
}
