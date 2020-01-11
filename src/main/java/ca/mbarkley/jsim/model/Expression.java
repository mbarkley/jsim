package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.prob.Event;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public abstract class Expression<T extends Comparable<T>> {
    protected Expression() {}

    public abstract Stream<Event<T>> events();

    public Map<T, Event<T>> calculateResults() {
        return events().collect(toMap(Event::getValue, identity()));
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
    }
}
