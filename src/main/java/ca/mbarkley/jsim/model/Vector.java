package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.model.Expression.Constant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@RequiredArgsConstructor
public class Vector implements Comparable<Vector> {
    @Getter
    private final Type.VectorType type;
    @Getter
    private final SortedMap<Symbol, Constant<?>> coordinate;

    public Constant<?> getCoordinate(Symbol symbol) {
        final Type<?> componentType = this.type.getDimensions().get(symbol);
        if (componentType != null) {
            return coordinate.getOrDefault(symbol, componentType.zeroAsConstant());
        } else {
            throw new IllegalArgumentException(format("Invalid symbol [%s] for vector type [%s]", symbol, this));
        }
    }

    @Override
    public int compareTo(Vector o) {
        return type.compare(this, o);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Vector && equals((Vector) o);
    }

    public boolean equals(Vector vector) {
        return compareTo(vector) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, coordinate);
    }

    @Override
    public String toString() {
        final String inner = coordinates().map(e -> format("%s=%s", e.getKey(), e.getValue().getValue()))
                                          .collect(Collectors.joining(", "));

        return format("{%s}", inner);
    }

    public Stream<Entry<Symbol, Constant<?>>> coordinates() {
        return type.getDimensions()
                   .entrySet()
                   .stream()
                   .map(e -> Map.entry(e.getKey(), coordinate.getOrDefault(e.getKey(), e.getValue().zeroAsConstant())));
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
