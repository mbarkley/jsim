package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.prob.Event;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static ca.mbarkley.jsim.model.ArithmeticOperators.lookupBinaryOp;
import static ca.mbarkley.jsim.prob.Event.productOfIndependent;
import static ca.mbarkley.jsim.util.FormatUtils.formatAsPercentage;
import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public abstract class Expression<T extends Comparable<T>> {
    protected Expression() {}

    public abstract Stream<Event<T>> events();

    public Map<T, Event<T>> calculateResults() {
        return events().collect(toMap(Event::getValue, identity()));
    }

    public abstract Type<T> getType();

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MultiplicativeExpression extends Expression<Vector> {
        Integer number;
        Expression<Vector> subExpression;

        @Override
        public Stream<Event<Vector>> events() {
            final List<Stream<Event<Vector>>> singleEventStreams = Stream.generate(() -> subExpression.events())
                                                                         .limit(number)
                                                                         .collect(toList());
            final BinaryOperator<Vector, Vector> op = (BinaryOperator<Vector, Vector>) lookupBinaryOp(subExpression.getType(), subExpression.getType(), "+").get();

            return productOfIndependent(singleEventStreams, op::evaluate);
        }

        @Override
        public String toString() {
            return format("(%s)", subExpression);
        }

        @Override
        public Type<Vector> getType() {
            return subExpression.getType();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Bracketed<T extends Comparable<T>> extends Expression<T> {
        Expression<T> subExpression;

        @Override
        public Stream<Event<T>> events() {
            return subExpression.events();
        }

        @Override
        public String toString() {
            return format("(%s)", subExpression);
        }

        @Override
        public Type<T> getType() {
            return subExpression.getType();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Constant<T extends Comparable<T>> extends Expression<T> {
        Type<T> type;
        T value;

        public Constant(Type<T> type, T value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public Stream<Event<T>> events() {
            return Stream.of(new Event<>(value, 1.0));
        }

        @Override
        public Type<T> getType() {
            return type;
        }

        @Override
        public String toString() {
            return "" + value;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class EventList<T extends Comparable<T>> extends Expression<T> {
        Type<T> type;
        List<Event<T>> values;

        @Override
        public Stream<Event<T>> events() {
            return values.stream();
        }

        @Override
        public Type<T> getType() {
            return type;
        }

        @Override
        public String toString() {
            final String[] valueString = values.stream()
                                               .map(event -> format("%s (%s)", event.getValue(), formatAsPercentage(event.getProbability())))
                                               .toArray(String[]::new);
            return format("[%s]", String.join(", ", valueString));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class ComparisonExpression<I extends Comparable<I>> extends Expression<Boolean> {
        Expression<I> left;
        BinaryOperator<I, Boolean> comparator;
        Expression<I> right;

        @Override
        public Stream<Event<Boolean>> events() {
            final Stream<Event<I>> left = getLeft().events();
            final Stream<Event<I>> right = getRight().events();

            return productOfIndependent(left,
                                        right,
                                        comparator::evaluate);
        }

        @Override
        public Type<Boolean> getType() {
            return Types.BOOLEAN_TYPE;
        }

        @Override
        public String toString() {
            return format("%s %s %s", left, comparator.getSymbol(), right);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class BinaryOpExpression<T extends Comparable<T>> extends Expression<T> {
        Type<T> type;
        Expression<T> left;
        BinaryOperator<T, T> operator;
        Expression<T> right;

        @Override
        public Stream<Event<T>> events() {
            return productOfIndependent(left.events(), right.events(), operator::evaluate);
        }

        @Override
        public Type<T> getType() {
            return type;
        }

        @Override
        public String toString() {
            return format("%s %s %s", left, operator.getSymbol(), right);
        }
    }
}
