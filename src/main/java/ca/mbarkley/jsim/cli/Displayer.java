package ca.mbarkley.jsim.cli;

import ca.mbarkley.jsim.prob.Event;
import ca.mbarkley.jsim.util.FormatUtils;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class Displayer {
    private final int desiredWidth;

    public <T extends Comparable<T>> String createSortedHistogram(String title, Stream<Event<T>> events) {
        return createSortedHistogram(title, events, comparing(Event::getValue, naturalOrder()));
    }

    private <T> String createSortedHistogram(String title, Stream<Event<T>> events, Comparator<Event<T>> comparator) {
        final List<Event<T>> sortedEvents = events.sorted(comparator)
                                                  .collect(toList());

        if (sortedEvents.isEmpty()) {
            throw new IllegalArgumentException("Cannot generate historam for empty event list.");
        } else {
            final double highestLikelihood = sortedEvents.stream()
                                                         .max(comparingDouble(Event::getProbability))
                                                         .get()
                                                         .getProbability();
            final int longestValueString = sortedEvents.stream()
                                                       .map(Event::getValue)
                                                       .map(Object::toString)
                                                       .map(String::length)
                                                       .max(naturalOrder())
                                                       .get();
            final int totalLeftPad = longestValueString + 1;

            final int minRightPadding = 5;
            final double charFactor = (desiredWidth - (double) totalLeftPad - "|".length() - minRightPadding) / highestLikelihood;

            final int largestPrintedCharLength = sortedEvents.stream()
                                                             .map(Event::getProbability)
                                                             .mapToInt(p -> charCount(charFactor, p))
                                                             .max()
                                                             .getAsInt();
            final int totalGraphSectionWidth = largestPrintedCharLength + minRightPadding;

            final StringBuilder sb = new StringBuilder();

            // Header
            final int barLength = max((desiredWidth - title.length() - 2) / 2, 0);
            final int remainderAdjustment = max((desiredWidth - title.length() - 2) % 2, 0);
            sb.append("-".repeat(barLength))
              .append(" ")
              .append(title)
              .append(" ")
              .append(" ".repeat(remainderAdjustment))
              .append("-".repeat(barLength))
              .append("\n");

            for (final Event<T> event : sortedEvents) {
                final double probability = event.getProbability();
                final int charCount = charCount(charFactor, probability);
                final String value = event.getValue().toString();
                sb.append(value)
                  .append(" ".repeat(totalLeftPad - value.length()))
                  .append('|')
                  .append("*".repeat(max(0, charCount)))
                  .append(" ".repeat(totalGraphSectionWidth - charCount))
                  .append(FormatUtils.formatAsPercentage(probability))
                  .append('\n');
            }

            return sb.toString();
        }
    }

    private int charCount(double charFactor, double probability) {
        return (int) Math.round(probability * charFactor);
    }
}
