package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.model.Vector.Dimension;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

import static java.lang.String.format;

public abstract class Type<T> implements Comparator<T> {
    public abstract T zero();
    public abstract String getName();

    @Override
    public String toString() {
        return getName();
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class VectorType extends Type<Vector> {
        SortedMap<String, Type<?>> dimensions;

        @Override
        public Vector zero() {
            return new Vector(this, List.of());
        }

        @Override
        public String getName() {
            return toString();
        }

        @Override
        public String toString() {
            return format("Dimensions{%s}", dimensions.entrySet()
                                                  .stream()
                                                  .map(e -> e.getKey() + " : " + e.getValue())
                                                  .collect(Collectors.joining(", ")));
        }

        @Override
        public int compare(Vector o1, Vector o2) {
            if (o1.getType().equals(this) && o2.getType().equals(this)) {
                return dimensions.entrySet()
                                 .stream()
                                 .map(dim -> {
                                     final Object v1 = lookupDimensionValue(o1, dim);
                                     final Object v2 = lookupDimensionValue(o2, dim);
                                     return unsafeCompare(v1, v2, dim.getValue());
                                 })
                                 .filter(n -> n != 0)
                                 .findFirst()
                                 .orElse(0);
            } else {
                throw new IllegalArgumentException(format("Cannot compare vectors [%s] and [%s] when one or more does not match this type [%s]", o1, o2, this));
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private int unsafeCompare(Object v1, Object v2, Comparator cmp) {
            return cmp.compare(v1, v2);
        }

        private Object lookupDimensionValue(Vector o1, Map.Entry<String, Type<?>> dim) {
            return o1.getCoordinate()
                     .stream()
                     .filter(d -> d.getName().equals(dim.getKey()))
                     .map(Dimension::getValue)
                     .map(Object.class::cast)
                     .findFirst()
                     .orElseGet(() -> dim.getValue().zero());
        }
    }

    @EqualsAndHashCode(callSuper = false)
    static class SymbolType extends Type<String> {

        @Override
        public String zero() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName() {
            return "Symbol";
        }

        @Override
        public int compare(String o1, String o2) {
            return o1.compareTo(o2);
        }
    }

    @EqualsAndHashCode(callSuper = false)
    static class IntegerType extends Type<Integer> {
        @Override
        public Integer zero() {
            return 0;
        }

        @Override
        public String getName() {
            return Integer.class.getSimpleName();
        }

        @Override
        public int compare(Integer o1, Integer o2) {
            return o1.compareTo(o2);
        }
    }

    @EqualsAndHashCode(callSuper = false)
    static class BooleanType extends Type<Boolean> {
        @Override
        public Boolean zero() {
            return false;
        }

        @Override
        public String getName() {
            return Boolean.class.getSimpleName();
        }

        @Override
        public int compare(Boolean o1, Boolean o2) {
            return o1.compareTo(o2);
        }
    }
}
