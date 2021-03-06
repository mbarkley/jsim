package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.prob.Event;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.stream.Stream;

import static ca.mbarkley.jsim.prob.Event.productOfIndependent;
import static java.lang.String.format;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.*;

public abstract class IntegerExpression extends Expression<Integer> {
    private IntegerExpression() {}

    @Override
    public Type<Integer> getType() {
        return Types.INTEGER_TYPE;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class HighDice extends IntegerExpression {
        HomogeneousDicePool dicePool;
        int numberOfDice;

        @Override
        public Stream<Event<Integer>> events(RuntimeContext ctx) {
            final List<Stream<Event<List<Integer>>>> singleDieStreams = Stream.generate(() -> Event.singleDieEvents(dicePool.getDiceSides())
                                                                                                   .map(event -> new Event<>(List.of(event.getValue()), event.getProbability())))
                                                                              .limit(dicePool.getNumberOfDice())
                                                                              .collect(toList());

            return productOfIndependent(singleDieStreams, this::updateValues)
                    .map(event -> new Event<>(event.getValue().stream().mapToInt(n -> n).sum(), event.getProbability()))
                    .collect(groupingBy(Event::getValue, reducing(0.0, Event::getProbability, Double::sum)))
                    .entrySet()
                    .stream()
                    .map(entry -> new Event<>(entry.getKey(), entry.getValue()));
        }

        @Override
        public boolean isConstant() {
            return dicePool.isConstant();
        }

        private List<Integer> updateValues(List<Integer> v1, List<Integer> v2) {
            return Stream.concat(v1.stream(), v2.stream())
                         .sorted(comparingInt(n -> (int) n).reversed())
                         .limit(numberOfDice)
                         .collect(toList());
        }

        @Override
        public String toString() {
            return format("%sH%d", dicePool, numberOfDice);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class LowDice extends IntegerExpression {
        HomogeneousDicePool dicePool;
        int numberOfDice;

        @Override
        public Stream<Event<Integer>> events(RuntimeContext ctx) {
            final List<Stream<Event<List<Integer>>>> singleDieStreams = Stream.generate(() -> Event.singleDieEvents(dicePool.getDiceSides())
                                                                                                   .map(event -> new Event<>(List.of(event.getValue()), event.getProbability())))
                                                                              .limit(dicePool.getNumberOfDice())
                                                                              .collect(toList());

            return productOfIndependent(singleDieStreams, this::updateValues)
                    .map(event -> new Event<>(event.getValue().stream().mapToInt(n -> n).sum(), event.getProbability()))
                    .collect(groupingBy(Event::getValue, reducing(0.0, Event::getProbability, Double::sum)))
                    .entrySet()
                    .stream()
                    .map(entry -> new Event<>(entry.getKey(), entry.getValue()));
        }

        @Override
        public boolean isConstant() {
            return dicePool.isConstant();
        }

        private List<Integer> updateValues(List<Integer> v1, List<Integer> v2) {
            return Stream.concat(v1.stream(), v2.stream())
                         .sorted(comparingInt(n -> (int) n))
                         .limit(numberOfDice)
                         .collect(toList());
        }

        @Override
        public String toString() {
            return format("%sL%d", dicePool, numberOfDice);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class HomogeneousDicePool extends IntegerExpression {
        int numberOfDice;
        int diceSides;

        @Override
        public Stream<Event<Integer>> events(RuntimeContext ctx) {
            final List<Stream<Event<Integer>>> singleDieStreams = Stream.generate(() -> Event.singleDieEvents(diceSides))
                                                                        .limit(numberOfDice)
                                                                        .collect(toList());

            return productOfIndependent(singleDieStreams, Integer::sum);
        }

        @Override
        public boolean isConstant() {
            return diceSides > 1;
        }

        @Override
        public String toString() {
            return format("%dd%d", numberOfDice, diceSides);
        }
    }
}
