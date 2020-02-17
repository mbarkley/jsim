package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.eval.EvaluationException.InvalidTypeException;
import ca.mbarkley.jsim.model.Expression.Constant;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public interface Type<T extends Comparable<T>> extends Comparator<T> {

    interface TypeClass<T extends Comparable<T>> {
        boolean isInstance(Type<?> type);
        Optional<Type<T>> unify(List<Type<T>> types);
    }

    T zero();
    String name();
    boolean isAssignableFrom(Type<?> type);
    TypeClass<T> typeClass();

    /**
     * Performs a cast with absolutely no type coercion. Throws an {@link InvalidTypeException} if
     * the given object does not exactly match this type.
     */
    T strictCast(Object object) throws InvalidTypeException;

    default boolean isAssignableTo(Type<?> type) {
        return type.isAssignableFrom(this);
    }

    default Expression<T> asType(Expression<?> expression) {
        if (this.isAssignableFrom(expression.getType())) {
            //noinspection unchecked
            return (Expression<T>) expression;
        } else {
            throw new InvalidTypeException(this, expression.getType());
        }
    }

    default Constant<T> zeroAsConstant() {
        return new Constant<>(this, zero());
    }

    abstract class SimpleType<T extends Comparable<T>> implements Type<T>, TypeClass<T> {
        @Override
        public boolean isAssignableFrom(Type<?> type) {
            return equals(type);
        }

        @Override
        public boolean isInstance(Type<?> type) {
            return equals(type);
        }

        @Override
        public TypeClass<T> typeClass() {
            return this;
        }

        @Override
        public String toString() {
            return name();
        }

        @Override
        public Optional<Type<T>> unify(List<Type<T>> types) {
            return types.stream()
                        .findFirst();
        }
    }

    @ToString
    @EqualsAndHashCode(callSuper = false)
    class VectorTypeClass implements TypeClass<Vector> {
        static final VectorTypeClass INSTANCE = new VectorTypeClass();
        private VectorTypeClass() {}

        @Override
        public boolean isInstance(Type<?> type) {
            return type instanceof VectorType;
        }

        @Override
        public Optional<Type<Vector>> unify(List<Type<Vector>> types) {
            final SortedMap<Symbol, Type<?>> dimensionSuperset = new TreeMap<>();
            for (var t : types) {
                final SortedMap<Symbol, Type<?>> dimensions = ((VectorType) t).getDimensions();
                for (var dim : dimensions.entrySet()) {
                    final Type<?> updated = dimensionSuperset.compute(dim.getKey(), (k, v) -> {
                        if (v == null || v.equals(dim.getValue())) {
                            return dim.getValue();
                        } else {
                            // Update null to indicate a conflict
                            return null;
                        }
                    });

                    // In case of conflicting component type, there is no way to unify
                    if (updated == null) {
                        return Optional.empty();
                    }
                }
            }

            return Optional.of(new VectorType(dimensionSuperset));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    class VectorType implements Type<Vector> {
        SortedMap<Symbol, Type<?>> dimensions;

        @Override
        public Vector zero() {
            return new Vector(this, new TreeMap<>());
        }

        @Override
        public String name() {
            return toString();
        }

        @Override
        public TypeClass<Vector> typeClass() {
            return VectorTypeClass.INSTANCE;
        }

        @Override
        public Vector strictCast(Object object) throws InvalidTypeException {
            if (object instanceof Vector) {
                Vector v = (Vector) object;
                if (v.getType().equals(this)) {
                    return v;
                }
            }

            throw new InvalidTypeException(this, object);
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
    @ToString
    class SymbolTypeClass implements TypeClass<Symbol> {
        static final SymbolTypeClass INSTANCE = new SymbolTypeClass();
        private SymbolTypeClass() {}

        @Override
        public boolean isInstance(Type<?> type) {
            return type instanceof SymbolType;
        }

        @Override
        public Optional<Type<Symbol>> unify(List<Type<Symbol>> types) {
            final List<Symbol> symbols = types.stream()
                                              .map(t -> ((SymbolType) t).getSymbol())
                                              .collect(toList());

            if (symbols.size() == 1) {
                return Optional.of(types.get(0));
            } else {
                return Optional.empty();
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    class SymbolType implements Type<Symbol> {
        private Symbol symbol;

        @Override
        public Symbol zero() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String name() {
            return format("Symbol[%s]", symbol);
        }

        @Override
        public TypeClass<Symbol> typeClass() {
            return SymbolTypeClass.INSTANCE;
        }

        @Override
        public Symbol strictCast(Object object) throws InvalidTypeException {
            if (object instanceof Symbol) {
                Symbol symbol = (Symbol) object;
                if (symbol.getType().equals(this)) {
                    return symbol;
                }
            }

            throw new InvalidTypeException(this, object);
        }

        @Override
        public boolean isAssignableFrom(Type<?> type) {
            return type instanceof SymbolType && this.equals(type);
        }

        @Override
        public int compare(Symbol o1, Symbol o2) {
            return o1.compareTo(o2);
        }

        @Override
        public String toString() {
            return name();
        }
    }

    @EqualsAndHashCode(callSuper = false)
    class IntegerType extends SimpleType<Integer> {
        @Override
        public Integer zero() {
            return 0;
        }

        @Override
        public String name() {
            return Integer.class.getSimpleName();
        }

        @Override
        public Integer strictCast(Object object) throws InvalidTypeException {
            if (object instanceof Integer) {
                return (Integer) object;
            } else {
                throw new InvalidTypeException(this, object);
            }
        }

        @Override
        public int compare(Integer o1, Integer o2) {
            return o1.compareTo(o2);
        }
    }

    @EqualsAndHashCode(callSuper = false)
    class BooleanType extends SimpleType<Boolean> {
        @Override
        public Boolean zero() {
            return false;
        }

        @Override
        public String name() {
            return Boolean.class.getSimpleName();
        }

        @Override
        public Boolean strictCast(Object object) throws InvalidTypeException {
            if (object instanceof Boolean) {
                return (Boolean) object;
            } else {
                throw new InvalidTypeException(this, object);
            }
        }

        @Override
        public int compare(Boolean o1, Boolean o2) {
            return o1.compareTo(o2);
        }
    }
}
