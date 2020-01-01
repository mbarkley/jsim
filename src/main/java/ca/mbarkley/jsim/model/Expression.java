package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.prob.Event;
import lombok.Value;

import java.util.List;
import java.util.stream.Stream;

import static ca.mbarkley.jsim.prob.Event.productOfIndependent;
import static java.lang.String.format;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.*;

public abstract class Expression extends Statement {
    private Expression() {}

    public abstract Stream<Event<Integer>> events();

    @Value
    public static class Constant extends Expression {
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
    public static class HighDice extends Expression {
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
    public static class LowDice extends Expression {
        HomogeneousDicePool dicePool;
        int numberOfDice;

        @Override
        public Stream<Event<Integer>> events() {
            return null;
        }

        @Override
        public String toString() {
            return format("%sL%d", dicePool, numberOfDice);
        }
    }

    @Value
    public static class HomogeneousDicePool extends Expression {
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
    public static class BinaryOpExpression extends Expression {
        Expression left;
        Operator operator;
        Expression right;

        @Override
        public Stream<Event<Integer>> events() {
            return productOfIndependent(left.events(), right.events(), operator::apply);
        }

        @Override
        public String toString() {
            return format("%s %s %s", left, operator, right);
        }
    }

    public enum Operator {
        PLUS("+") {
            @Override
            public int apply(int left, int right) {
                return left + right;
            }
        }, MINUS("-") {
            @Override
            public int apply(int left, int right) {
                return left - right;
            }
        };

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }

        public abstract int apply(int left, int right);
    }
}
