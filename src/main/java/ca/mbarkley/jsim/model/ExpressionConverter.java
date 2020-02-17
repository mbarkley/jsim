package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.eval.EvaluationException.InvalidTypeException;
import ca.mbarkley.jsim.model.Expression.MappedExpression;
import ca.mbarkley.jsim.model.Type.TypeClass;
import ca.mbarkley.jsim.model.Type.VectorType;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static ca.mbarkley.jsim.model.Types.*;

public abstract class ExpressionConverter<S extends Comparable<S>, T extends Comparable<T>> {

    @Value
    public static class ConverterKey {
        TypeClass<?> source;
        TypeClass<?> target;
    }

    /**
     * Implementations must define equality such that for every source two converters {@code c1} and {@code c2},
     * if for all source values {@code s}:
     * <pre><code>
     *     c1.convert(s).equals(c2.convert(s))
     * </code></pre>
     * then it must be the case that {@code c1.equals(c2)}
     * @param <S> Source type
     * @param <T> Target type
     */
    public interface ValueConverter<S extends Comparable<S>, T extends Comparable<T>> {
        T convert(S value);
        Type<T> getTargetType();
    }

    public abstract Expression<T> convert(Expression<S> expression, Type<T> targetType);
    public abstract Type<T> convert(Type<S> source);

    public static final Map<ConverterKey, ExpressionConverter<?, ?>> converters = Map.of(
            new ConverterKey(SYMBOL_TYPE_CLASS, VECTOR_TYPE_CLASS), new SymbolToVectorExpressionConverter(),

            new ConverterKey(SYMBOL_TYPE_CLASS, SYMBOL_TYPE_CLASS), new IdentityConverter<>(),
            new ConverterKey(VECTOR_TYPE_CLASS, VECTOR_TYPE_CLASS), new VectorToVectorExpressionConverter(),
            new ConverterKey(INTEGER_TYPE_CLASS, INTEGER_TYPE_CLASS), new IdentityConverter<>(),
            new ConverterKey(BOOLEAN_TYPE_CLASS, BOOLEAN_TYPE_CLASS), new IdentityConverter<>()
    );

    @Value
    private static class VectorToVectorValueConverter implements ValueConverter<Vector, Vector> {
        Type<Vector> targetType;

        @Override
        public Vector convert(Vector source) {
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
    }

    private static class VectorToVectorExpressionConverter extends ExpressionConverter<Vector, Vector> {
        @Override
        public Expression<Vector> convert(Expression<Vector> source, Type<Vector> targetType) {
            if (source.getType().equals(targetType)) {
                return source;
            } else {
                VECTOR_TYPE_CLASS.unify(List.of(source.getType(), targetType))
                                 .filter(t -> t.equals(targetType))
                                 .orElseThrow(() -> new InvalidTypeException(source.getType(), targetType));

                return new MappedExpression<>(source, new VectorToVectorValueConverter(targetType));
            }
        }

        @Override
        public Type<Vector> convert(Type<Vector> source) {
            return source;
        }
    }

    @Value
    private static class SymbolToVectorValueConverter implements ValueConverter<Symbol, Vector> {
        VectorType targetType;

        @Override
        public Vector convert(Symbol source) {
            if (targetType.getDimensions().containsKey(source)) {
                return new Vector(targetType, new TreeMap<>(Map.of(source, Constants.of(1))));
            } else {
                throw new InvalidTypeException(source.getType(), targetType);
            }
        }
    }

    private static class SymbolToVectorExpressionConverter extends ExpressionConverter<Symbol, Vector> {
        @Override
        public Expression<Vector> convert(Expression<Symbol> source, Type<Vector> target) {
            final VectorType vectorType = (VectorType) target;
            final Type.SymbolType symbolType = (Type.SymbolType) source.getType();
            if (vectorType.getDimensions().containsKey(symbolType.getSymbol())) {
                return new MappedExpression<>(source, new SymbolToVectorValueConverter((VectorType) target));
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

    private static class IdentityConverter<T extends Comparable<T>> extends ExpressionConverter<T, T> {
        @Override
        public Expression<T> convert(Expression<T> source, Type<T> target) {
            return source;
        }

        @Override
        public Type<T> convert(Type<T> source) {
            return source;
        }
    }
}
