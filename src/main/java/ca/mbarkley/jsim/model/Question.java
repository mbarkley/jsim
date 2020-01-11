package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.prob.Event;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

import java.util.Optional;
import java.util.stream.Stream;

import static ca.mbarkley.jsim.prob.Event.productOfIndependent;
import static java.lang.String.format;

public abstract class Question extends Statement<Boolean> {

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class BinaryOpQuestion extends Question {
        Question left;
        BooleanOperator operator;
        Question right;

        @Override
        public Stream<Event<Boolean>> events() {
            final Stream<Event<Boolean>> left = getLeft().events();
            final Stream<Event<Boolean>> right = getRight().events();

            return productOfIndependent(left,
                                        right,
                                        operator::evaluate);
        }

        @Override
        public String toString() {
            return format("%s %s %s", left, operator, right);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class BinaryBooleanExpression extends Question {
        Expression left;
        Comparator comparator;
        Expression right;

        @Override
        public Stream<Event<Boolean>> events() {
            final Stream<Event<Integer>> left = getLeft().events();
            final Stream<Event<Integer>> right = getRight().events();

            return productOfIndependent(left,
                                        right,
                                        comparator::evaluate);
        }

        @Override
        public String toString() {
            return format("%s %s %s", left, comparator, right);
        }
    }


    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class BooleanConstant extends Question {
        public static BooleanConstant TRUE = new BooleanConstant(true);
        public static BooleanConstant FALSE = new BooleanConstant(false);

        boolean value;

        @Override
        public Stream<Event<Boolean>> events() {
            return Stream.of(new Event<>(value, 1.0));
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public enum BooleanOperator implements HasSymbol<BooleanOperator> {
        AND("and", 1) {
            @Override
            public boolean evaluate(boolean left, boolean right) {
                return left && right;
            }
        },
        OR("or", 0) {
            @Override
            public boolean evaluate(boolean left, boolean right) {
                return left || right;
            }
        };

        @Getter
        private final String symbol;
        @Getter
        private final int precedent;

        BooleanOperator(String symbol, int precedent) {
            this.symbol = symbol;
            this.precedent = precedent;
        }

        public abstract boolean evaluate(boolean left, boolean right);

        @Override
        public String toString() {
            return symbol;
        }

        public static Optional<BooleanOperator> lookup(String symbol) {
            return HasSymbol.lookup(symbol, values());
        }
    }

    public enum Comparator implements HasSymbol<Comparator> {
        LT("<") {
            @Override
            public boolean evaluate(int left, int right) {
                return left < right;
            }
        }, GT(">") {
            @Override
            public boolean evaluate(int left, int right) {
                return left > right;
            }
        }, EQ("=") {
            @Override
            public boolean evaluate(int left, int right) {
                return left == right;
            }
        };

        @Getter
        private final String symbol;

        Comparator(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }

        public abstract boolean evaluate(int left, int right);

        public static Optional<Comparator> lookup(String symbol) {
            return HasSymbol.lookup(symbol, values());
        }
    }
}
