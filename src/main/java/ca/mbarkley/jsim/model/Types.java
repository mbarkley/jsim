package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.eval.EvaluationException;
import ca.mbarkley.jsim.eval.EvaluationException.TypeUnificationException;
import ca.mbarkley.jsim.model.ExpressionConverter.ConverterKey;
import ca.mbarkley.jsim.model.Type.TypeClass;
import ca.mbarkley.jsim.model.Type.VectorType;

import java.util.*;

import static ca.mbarkley.jsim.model.ExpressionConverter.converters;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public abstract class Types {
    public static final Type<Integer> INTEGER_TYPE = new Type.IntegerType();
    public static final Type<Boolean> BOOLEAN_TYPE = new Type.BooleanType();
    public static final VectorType EMPTY_VECTOR_TYPE = new VectorType(new TreeMap<>());

    public static final TypeClass<Integer> INTEGER_TYPE_CLASS = new Type.IntegerType();
    public static final TypeClass<Boolean> BOOLEAN_TYPE_CLASS = new Type.BooleanType();
    public static final TypeClass<Vector> VECTOR_TYPE_CLASS = Type.VectorTypeClass.INSTANCE;
    public static final TypeClass<Symbol> SYMBOL_TYPE_CLASS = Type.SymbolTypeClass.INSTANCE;

    public static Type.SymbolType symbolTypeOf(Symbol symbol) {
        return new Type.SymbolType(symbol);
    }

    public static VectorType vectorTypeOf(Map<Symbol, ? extends Type<?>> componentTypes) {
        return new VectorType(new TreeMap<>(componentTypes));
    }

    public static VectorType mergeVectorTypes(Collection<VectorType> types) throws EvaluationException.UnmergableVectorTypeException {
        final SortedMap<Symbol, Type<?>> dimensionSuperset = new TreeMap<>();
        for (var t : types) {
            final SortedMap<Symbol, Type<?>> dimensions = t.getDimensions();
            for (var dim : dimensions.entrySet()) {
                dimensionSuperset.compute(dim.getKey(), (k, v) -> {
                    if (v == null || v.equals(dim.getValue())) {
                        return dim.getValue();
                    } else {
                        throw new EvaluationException.UnmergableVectorTypeException(format("Dimension [%s] must have one type, but found [%s] and [%s]", dim.getKey(), v, dim.getValue()));
                    }
                });
            }
        }

        return new VectorType(dimensionSuperset);
    }

    public static Optional<Type<?>> findCommonType(Type<?>... types) {
        final Set<Type<?>> typeSet = new HashSet<>(List.of(types));
        try {
            return Optional.of(findCommonType(typeSet));
        } catch (TypeUnificationException tue) {
            return Optional.empty();
        }
    }

    public static Type<?> findCommonType(Set<? extends Type<?>> types) throws EvaluationException {
        if (types.size() > 1) {
            final Set<? extends TypeClass<?>> targets = types.stream()
                                                             .map(type -> converters.keySet()
                                                                                    .stream()
                                                                                    .filter(key -> key.getSource().isInstance(type))
                                                                                    .map(ConverterKey::getTarget)
                                                                                    .collect(toSet()))
                                                             .reduce((left, right) -> left.stream()
                                                                                          .filter(right::contains)
                                                                                          .collect(toSet()))
                                                             .get();

            final Set<? extends Type<?>> viableTargets = targets.stream()
                                                     .map(targetTypeClass -> {
                                                         final List<Type> convertedTypes = types.stream()
                                                                                                .map(type -> {
                                                                                                    final ConverterKey key = new ConverterKey(type.typeClass(), targetTypeClass);
                                                                                                    final ExpressionConverter<?, ?> converter = converters.get(key);

                                                                                                    return converter.convert((Type) type);
                                                                                                })
                                                                                                .collect(toList());

                                                         return (Optional<Type<?>>) ((TypeClass) targetTypeClass).unify(convertedTypes);
                                                     })
                                                     .filter(Optional::isPresent)
                                                     .map(Optional::get)
                                                     .collect(toSet());

            if (viableTargets.size() == 1) {
                return viableTargets.iterator().next();
            } else {
                throw new TypeUnificationException(types, viableTargets);
            }
        } else {
            return types.iterator().next();
        }
    }

    private Types() {}

    public static Expression<?> convertExpression(Expression<?> expression, Type<?> commonType) {
        final ExpressionConverter converter = converters.get(new ConverterKey(expression.getType().typeClass(), commonType.typeClass()));
        return converter.convert(expression, commonType);
    }
}
