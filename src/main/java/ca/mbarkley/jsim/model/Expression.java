package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.prob.Event;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public abstract class Expression<T extends Comparable<T>> {
    protected Expression() {}

    public abstract Stream<Event<T>> events();

    public Map<T, Event<T>> calculateResults() {
        return events().collect(toMap(Event::getValue, identity()));
    }
}
