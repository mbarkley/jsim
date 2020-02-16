package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.eval.EvaluationException;
import ca.mbarkley.jsim.eval.EvaluationException.InvalidTypeException;
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

    public static final BinaryOperator<Integer, Integer> intAddition = BinaryOperator.create(Types.INTEGER_TYPE, "+", Integer::sum);
    public static final BinaryOperator<Integer, Integer> intSubtraction = BinaryOperator.create(Types.INTEGER_TYPE, "-", (l, r) -> l-r);
    public static final BinaryOperator<Integer, Integer> multiplication = BinaryOperator.create(Types.INTEGER_TYPE,"*", (l, r) -> l*r);
    public static final BinaryOperator<Integer, Integer> division = BinaryOperator.create(Types.INTEGER_TYPE, "/", (l, r) -> l/r);

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> Optional<? extends BinaryOperator<T, T>> lookupBinaryOp(Type<?> left, Type<?> right, String symbol) {
        switch (symbol) {
            case "*":
                if (Types.INTEGER_TYPE.isAssignableFrom(left) && Types.INTEGER_TYPE.isAssignableFrom(right)) {
                    return (Optional) Optional.of(multiplication);
                }
                break;
            case "/":
                if (Types.INTEGER_TYPE.isAssignableFrom(left) && Types.INTEGER_TYPE.isAssignableFrom(right)) {
                    return (Optional) Optional.of(division);
                }
                break;
            case "+":
                if (Types.INTEGER_TYPE.isAssignableFrom(left) && Types.INTEGER_TYPE.isAssignableFrom(right)) {
                    return (Optional) Optional.of(intAddition);
                } else if (Types.EMPTY_VECTOR_TYPE.isAssignableTo(left) && Types.EMPTY_VECTOR_TYPE.isAssignableTo(right)) {
                    return (Optional) Optional.of(new VectorBinaryOperation(symbol));
                }
                break;
            case "-":
                if (Types.INTEGER_TYPE.isAssignableFrom(left) && Types.INTEGER_TYPE.isAssignableFrom(right)) {
                    return (Optional) Optional.of(intSubtraction);
                } else if (left.isAssignableFrom(Types.EMPTY_VECTOR_TYPE) && right.isAssignableFrom(Types.EMPTY_VECTOR_TYPE)) {
                    return (Optional) Optional.of(new VectorBinaryOperation(symbol));
                }
        }

        return Optional.empty();
    }

    private static class VectorBinaryOperation implements BinaryOperator<Vector, Vector> {
        private final String symbol;

        public VectorBinaryOperation(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public Vector evaluate(Vector left, Vector right) {
            final VectorType mergedType = mergeVectorTypes(List.of(left.getType(), right.getType()));
            final SortedMap<Symbol, Constant<?>>  newCoordinates = new TreeMap<>();
            mergedType.getDimensions()
                      .forEach((name, type) -> newCoordinates.put(name, applyDimensionOp(left, right, name, type)));

            return new Vector(mergedType, newCoordinates);
        }

        @Override
        public Type<Vector> getOutputType(Type<Vector> left, Type<Vector> right) {
            if (left instanceof VectorType && right instanceof VectorType) {
                return mergeVectorTypes(List.of((VectorType) left, (VectorType) right));
            } else {
                throw new InvalidTypeException(format("Expected left and right to be Vector types, but were [%s] and [%s]", left.name(), right.name()));
            }
        }

        @SuppressWarnings("unchecked")
        private <T extends Comparable<T>> Constant<T> applyDimensionOp(Vector left, Vector right, Symbol name, Type<T> outputType) {
            final Constant<T> leftValue = (Constant<T>) left.getCoordinate()
                                                            .getOrDefault(name, outputType.zeroAsConstant());
            final Constant<T> rightValue = (Constant<T>) right.getCoordinate()
                                                              .getOrDefault(name, outputType.zeroAsConstant());

            return applyDimensionOp(leftValue.getType(), rightValue.getType(), outputType, leftValue, rightValue);
        }

        private <T extends Comparable<T>> Constant<T> applyDimensionOp(Type<T> left, Type<T> right, Type<T> outputType, Constant<T> leftValue, Constant<T> rightValue) {
            final BinaryOperator<T, T> op =
                    (BinaryOperator<T, T>) lookupBinaryOp(left, right, symbol).orElseThrow(() -> new EvaluationException(format("Operator [%s] is undefined for types [%s, %s]", symbol, left, right)));
            final T newValue = op.evaluate(leftValue.getValue(), rightValue.getValue());

            return new Constant<T>(outputType, newValue);
        }

        @Override
        public String getSymbol() {
            return symbol;
        }
    }
}
