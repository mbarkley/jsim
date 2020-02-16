package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.eval.EvaluationException.InvalidTypeException;
import ca.mbarkley.jsim.model.Type.TypeClass;
import ca.mbarkley.jsim.model.Type.VectorType;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static ca.mbarkley.jsim.model.Types.*;

public abstract class Converter<S extends Comparable<S>, T extends Comparable<T>> {

    @Value
    public static class ConverterKey {
        TypeClass<?> source;
        TypeClass<?> target;
    }

    public abstract T convert(S source, Type<T> targetType);
    public abstract Type<T> convert(Type<S> source);

    public static final Map<ConverterKey, Converter<?, ?>> converters = Map.of(
            new ConverterKey(SYMBOL_TYPE_CLASS, VECTOR_TYPE_CLASS), new SymbolToVector(),

            new ConverterKey(SYMBOL_TYPE_CLASS, SYMBOL_TYPE_CLASS), new IdentityConverter<>(),
            new ConverterKey(VECTOR_TYPE_CLASS, VECTOR_TYPE_CLASS), new VectorToVector(),
            new ConverterKey(INTEGER_TYPE_CLASS, INTEGER_TYPE_CLASS), new IdentityConverter<>(),
            new ConverterKey(BOOLEAN_TYPE_CLASS, BOOLEAN_TYPE_CLASS), new IdentityConverter<>()
    );

    private static class VectorToVector extends Converter<Vector, Vector> {
        @Override
        public Vector convert(Vector source, Type<Vector> targetType) {
            VECTOR_TYPE_CLASS.unify(List.of(source.getType(), targetType))
                             .filter(t -> t.equals(targetType))
                             .orElseThrow(() -> new InvalidTypeException(source.getType(), targetType));

            final SortedMap<Symbol, Expression.Constant<?>> coordinates;
            final VectorType vectorType = (VectorType) targetType;
            if (source.getCoordinate().keySet().size() == vectorType.getDimensions().size()) {
                coordinates = source.getCoordinate();
            } else {
                coordinates = new TreeMap<>(source.getCoordinate());
                for (var e : vectorType.getDimensions().entrySet()) {
                    if (!coordinates.containsKey(e.getKey())) {
                        coordinates.put(e.getKey(), e.getValue().zeroAsConstant());
                    }
                }
            }

            return new Vector(vectorType, coordinates);
        }

        @Override
        public Type<Vector> convert(Type<Vector> source) {
            return source;
        }
    }

    private static class SymbolToVector extends Converter<Symbol, Vector> {
        @Override
        public Vector convert(Symbol source, Type<Vector> target) {
            final VectorType vectorType = (VectorType) target;
            if (vectorType.getDimensions().containsKey(source)) {
                return new Vector(vectorType, new TreeMap<>(Map.of(source, Constants.of(1))));
            } else {
                throw new InvalidTypeException(source.getType(), target);
            }
        }

        @Override
        public Type<Vector> convert(Type<Symbol> source) {
            final Symbol symbol = ((Type.SymbolType) source).getSymbol();
            return Types.vectorTypeOf(Map.of(symbol, INTEGER_TYPE));
        }
    }

    private static class IdentityConverter<T extends Comparable<T>> extends Converter<T, T> {
        @Override
        public T convert(T source, Type<T> target) {
            return source;
        }

        @Override
        public Type<T> convert(Type<T> source) {
            return source;
        }
    }
}
