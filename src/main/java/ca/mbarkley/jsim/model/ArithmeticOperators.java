package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.eval.EvaluationException;
import ca.mbarkley.jsim.model.Expression.Constant;
import ca.mbarkley.jsim.model.Type.VectorType;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import static ca.mbarkley.jsim.model.Types.mergeVectorTypes;
import static java.lang.String.format;

public abstract class ArithmeticOperators {
    private ArithmeticOperators() {}

    public static final BinaryOperator<Integer, Integer> intAddition = BinaryOperator.create("+", Integer::sum);
    public static final BinaryOperator<Integer, Integer> intSubtraction = BinaryOperator.create("-", (l, r) -> l-r);
    public static final BinaryOperator<Integer, Integer> multiplication = BinaryOperator.create("*", (l, r) -> l*r);
    public static final BinaryOperator<Integer, Integer> division = BinaryOperator.create("/", (l, r) -> l/r);

    @SuppressWarnings("unchecked")
    public static <V extends Comparable<V>, T extends Type<V>> Optional<? extends BinaryOperator<V, V>> lookupOp(T type, String symbol) {
        if (type instanceof VectorType) {
            return (Optional) lookupVectorOp(symbol);
        } else if (type instanceof Type.IntegerType) {
            return (Optional) lookupIntegerOp(symbol);
        } else {
            return Optional.empty();
        }
    }

    public static Optional<BinaryOperator<Integer, Integer>> lookupIntegerOp(String symbol) {
        return HasSymbol.lookup(symbol, List.of(intAddition, intSubtraction, multiplication, division));
    }

    public static Optional<BinaryOperator<Vector, Vector>> lookupVectorOp(String symbol) {
        switch (symbol) {
            case "+":
            case "-":
                return Optional.of(new VectorBinaryOperation(symbol));
            default:
                return Optional.empty();
        }
    }

    private static class VectorBinaryOperation implements BinaryOperator<Vector, Vector> {
        private final String symbol;

        public VectorBinaryOperation(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public Vector evaluate(Vector left, Vector right) {
            final VectorType mergedType = mergeVectorTypes(List.of(left.getType(), right.getType()));
            final SortedMap<String, Constant<?>>  newCoordinates = new TreeMap<>();
            mergedType.getDimensions()
                      .entrySet()
                      .forEach(e -> {
                          final String name = e.getKey();
                          final Type<?> type = e.getValue();

                          newCoordinates.put(name, applyDimensionOp(left, right, name, type));
                      });

            return new Vector(mergedType, newCoordinates);
        }

        @SuppressWarnings("unchecked")
        private <T extends Comparable<T>> Constant<T> applyDimensionOp(Vector left, Vector right, String name, Type<T> type) {
            final Constant<T> leftValue = (Constant<T>) left.getCoordinate()
                                                            .getOrDefault(name, type.zeroAsConstant());
            final Constant<T> rightValue = (Constant<T>) right.getCoordinate()
                                                              .getOrDefault(name, type.zeroAsConstant());

            return applyDimensionOp(type, leftValue, rightValue);
        }

        private <T extends Comparable<T>> Constant<T> applyDimensionOp(Type<T> type, Constant<T> leftValue, Constant<T> rightValue) {
            final BinaryOperator<T, T> op =
                    lookupOp(type, symbol).orElseThrow(() -> new EvaluationException(format("Operator [%s] is undefined for type [%s]", symbol, type)));
            final T newValue = op.evaluate(leftValue.getValue(), rightValue.getValue());

            return new Constant<T>(type, newValue);
        }

        @Override
        public String getSymbol() {
            return symbol;
        }
    }
}
