package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.prob.Event;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static ca.mbarkley.jsim.prob.Event.productOfIndependent;
import static java.lang.String.format;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.*;

public abstract class IntegerExpression extends Expression<Integer> {
    private IntegerExpression() {}

    @Override
    public Class<Integer> getType() {
        return Integer.class;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Constant extends IntegerExpression {
        int value;

        @Override
        public Stream<Event<Integer>> events() {
            return Stream.of(new Event<>(value, 1.0));
        }

        @Override
        public String toString() {
            return "" + value;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class HighDice extends IntegerExpression {
        HomogeneousDicePool dicePool;
        int numberOfDice;

        @Override
        public Stream<Event<Integer>> events() {
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
        public Stream<Event<Integer>> events() {
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
        public Stream<Event<Integer>> events() {
            final List<Stream<Event<Integer>>> singleDieStreams = Stream.generate(() -> Event.singleDieEvents(diceSides))
                                                                        .limit(numberOfDice)
                                                                        .collect(toList());

            return productOfIndependent(singleDieStreams, Integer::sum);
        }

        @Override
        public String toString() {
            return format("%dd%d", numberOfDice, diceSides);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class BinaryOpExpression extends IntegerExpression {
        Expression<Integer> left;
        Operator operator;
        Expression<Integer> right;

        @Override
        public Stream<Event<Integer>> events() {
            return productOfIndependent(left.events(), right.events(), operator::apply);
        }

        @Override
        public String toString() {
            return format("%s %s %s", left, operator, right);
        }
    }

    public enum Operator implements HasSymbol<Operator>, Comparable<Operator> {
        DIVIDE("/", 2) {
            @Override
            public int apply(int left, int right) {
                return left / right;
            }
        },
        TIMES("*", 1) {
            @Override
            public int apply(int left, int right) {
                return left * right;
            }
        },
        PLUS("+", 0) {
            @Override
            public int apply(int left, int right) {
                return left + right;
            }
        },
        MINUS("-", 0) {
            @Override
            public int apply(int left, int right) {
                return left - right;
            }
        };

        @Getter
        private final String symbol;
        @Getter
        private final int precedent;

        Operator(String symbol, int precedent) {
            this.symbol = symbol;
            this.precedent = precedent;
        }

        @Override
        public String toString() {
            return symbol;
        }

        public abstract int apply(int left, int right);

        public static Optional<Operator> lookup(String symbol) {
            return HasSymbol.lookup(symbol, values());
        }
    }
}