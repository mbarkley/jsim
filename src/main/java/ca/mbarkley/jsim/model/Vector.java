package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.model.Expression.Constant;
import lombok.Value;

import java.util.SortedMap;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Value
public class Vector implements Comparable<Vector> {
    Type.VectorType type;
    SortedMap<String, Constant<?>> coordinate;

    @Override
    public int compareTo(Vector o) {
        return type.compare(this, o);
    }

    @Override
    public String toString() {
        final String inner = type.getDimensions()
                                 .entrySet()
                                 .stream()
                                 .map(e -> {
                                     final Constant<?> value = coordinate.get(e.getKey());
                                     if (value != null) {
                                         return e.getKey() + "=" + value.toString();
                                     } else {
                                         return e.getKey() + "=" + e.getValue().zero().toString();
                                     }
                                 })
                                 .collect(Collectors.joining(", "));

        return format("{%s}", inner);
    }

    @Value
    public static class Dimension<T extends Comparable<T>> implements Comparable<Dimension<T>> {
        String name;
        Constant<T> value;

        @Override
        public int compareTo(Dimension<T> o) {
            return name.compareTo(o.name);
        }
    }
}
