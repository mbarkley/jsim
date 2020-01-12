package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.prob.Event;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static ca.mbarkley.jsim.prob.Event.productOfIndependent;
import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public abstract class Expression<T extends Comparable<T>> {
    protected Expression() {}

    public abstract Stream<Event<T>> events();

    public Map<T, Event<T>> calculateResults() {
        return events().collect(toMap(Event::getValue, identity()));
    }

    public abstract Class<T> getType();

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
        public Class<T> getType() {
            return subExpression.getType();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Constant<T extends Comparable<T>> extends Expression<T> {
        T value;

        @Override
        public Stream<Event<T>> events() {
            return Stream.of(new Event<>(value, 1.0));
        }

        @Override
        public Class<T> getType() {
            //noinspection unchecked
            return (Class<T>) value.getClass();
        }

        @Override
        public String toString() {
            return "" + value;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class EventList<T extends Comparable<T>> extends Expression<T> {
        Class<T> type;
        List<Event<T>> values;

        @Override
        public Stream<Event<T>> events() {
            return values.stream();
        }

        @Override
        public Class<T> getType() {
            return type;
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
        public Class<Boolean> getType() {
            return Boolean.class;
        }

        @Override
        public String toString() {
            return format("%s %s %s", left, comparator.getSymbol(), right);
        }
    }
}
