package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.eval.EvaluationException.InvalidTypeException;
import ca.mbarkley.jsim.model.Expression.Constant;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.lang.String.format;

public abstract class Type<T extends Comparable<T>> implements Comparator<T> {
    public abstract T zero();
    public abstract String getName();
    public abstract Expression<T> asType(Expression<?> expression);
    public abstract boolean isAssignableFrom(Type<?> type);

    public Constant<T> zeroAsConstant() {
        return new Constant<>(this, zero());
    }

    @Override
    public String toString() {
        return getName();
    }

    public abstract static class SimpleType<T extends Comparable<T>> extends Type<T> {
        public Expression<T> asType(Expression<?> expression) throws InvalidTypeException {
            final Type<?> type = expression.getType();
            if (this.equals(type)) {
                //noinspection unchecked
                return (Expression<T>) expression;
            } else {
                throw new InvalidTypeException(this, type);
            }
        }

        @Override
        public boolean isAssignableFrom(Type<?> type) {
            return this.equals(type);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class VectorType extends Type<Vector> {
        SortedMap<Symbol, Type<?>> dimensions;

        @Override
        public Vector zero() {
            return new Vector(this, new TreeMap<>());
        }

        @Override
        public String getName() {
            return toString();
        }

        @Override
        public Expression<Vector> asType(Expression<?> expression) {
            if (this.isAssignableFrom(expression.getType())) {
                //noinspection unchecked
                return (Expression<Vector>) expression;
            } else {
                throw new InvalidTypeException(this, expression.getType());
            }
        }

        @Override
        public boolean isAssignableFrom(Type<?> type) {
            return type instanceof VectorType
                    && ((VectorType) type).getDimensions()
                                          .entrySet()
                                          .stream()
                                          .allMatch(e -> {
                                              final Type<?> requiredDimType = dimensions.get(e.getKey());
                                              return requiredDimType != null && requiredDimType.isAssignableFrom(e.getValue());
                                          });
        }

        @Override
        public String toString() {
            return format("Vector{%s}", dimensions.entrySet()
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

        private Object lookupDimensionValue(Vector o1, Map.Entry<Symbol, Type<?>> dim) {
            return o1.getCoordinate()
                     .getOrDefault(dim.getKey(), dim.getValue().zeroAsConstant())
                     .getValue();
        }
    }

    @EqualsAndHashCode(callSuper = false)
    static class SymbolType extends SimpleType<Symbol> {

        @Override
        public Symbol zero() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName() {
            return "Symbol";
        }

        @Override
        public int compare(Symbol o1, Symbol o2) {
            return o1.compareTo(o2);
        }
    }

    @EqualsAndHashCode(callSuper = false)
    static class IntegerType extends SimpleType<Integer> {
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
    static class BooleanType extends SimpleType<Boolean> {
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
