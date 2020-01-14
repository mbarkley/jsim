package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.eval.EvaluationException;
import ca.mbarkley.jsim.model.Type.VectorType;

import java.util.*;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

public abstract class Types {
    public static final Type<Integer> INTEGER_TYPE = new Type.IntegerType();
    public static final Type<Boolean> BOOLEAN_TYPE = new Type.BooleanType();
    public static final Type<String> SYMBOL_TYPE = new Type.SymbolType();
    public static final Type<Vector> EMPTY_VECTOR_TYPE = new VectorType(new TreeMap<>());

    private Types() {}

    public static VectorType mergeVectorTypes(Collection<VectorType> types) throws EvaluationException.UnmergableVectorTypeException {
        final SortedMap<String, Type<?>> dimensionSuperset = new TreeMap<>();
        for (var t : types) {
            final SortedMap<String, Type<?>> dimensions = t.getDimensions();
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

    public static Type<?> findCommonType(Set<? extends Type<?>> types) throws EvaluationException {
        if (types.size() > 1) {
            if (types.stream().allMatch(t -> t instanceof Type.VectorType)) {
                //noinspection unchecked
                return mergeVectorTypes((Set<VectorType>) types);
            } else {
                throw new EvaluationException.DiceTypeException(types.stream()
                                                                     .map(Type::getName)
                                                                     .collect(toSet()));
            }
        } else {
            return types.iterator().next();
        }
    }
}
