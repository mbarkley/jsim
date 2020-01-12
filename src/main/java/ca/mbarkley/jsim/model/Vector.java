package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.model.Expression.Constant;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Value
public class Vector implements Comparable<Vector> {
    Type.VectorType type;
    List<Dimension<?>> coordinate;

    public Vector(Type.VectorType type, List<Dimension<?>> coordinate) {
        this.type = type;
        this.coordinate = coordinate;
    }

    @Override
    public int compareTo(Vector o) {
        return type.compare(this, o);
    }

    @Override
    public String toString() {
        final Map<String, Constant<?>> values = coordinate.stream()
                                                       .collect(Collectors.toMap(Dimension::getName, Dimension::getValue));
        final String inner = type.getDimensions()
                                 .entrySet()
                                 .stream()
                                 .map(e -> {
                                     final Constant<?> value = values.get(e.getKey());
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
