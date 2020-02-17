package ca.mbarkley.jsim.model;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Objects;
import java.util.function.BiFunction;

public interface BinaryOperator<I extends Comparable<I>, O extends Comparable<O>> extends HasSymbol<BinaryOperator<I, O>> {
    O evaluate(I left, I right);
    Type<O> getOutputType(Type<I> leftType, Type<I> rightType);

    static <T extends Comparable<T>> BinaryOperator<T, Boolean> strictEquality() {
        return new StrictEquality<>();
    }

    static <I extends Comparable<I>, O extends Comparable<O>> BinaryOperator<I, O> create(Type<O> type, String symbol, BiFunction<I, I, O> operator) {
        return new SimpleBinaryOperator<I, O>(type, symbol, operator);
    }

    @Value
    class StrictEquality<T extends Comparable<T>> implements BinaryOperator<T, Boolean> {
        @Override
        public Boolean evaluate(T left, T right) {
            return Objects.equals(left, right);
        }

        @Override
        public Type<Boolean> getOutputType(Type<T> left, Type<T> right) {
            return Types.BOOLEAN_TYPE;
        }

        @Override
        public String getSymbol() {
            return "=";
        }
    }

    @Value
    @EqualsAndHashCode(exclude = "operator")
    class SimpleBinaryOperator<I extends Comparable<I>, O extends Comparable<O>> implements BinaryOperator<I, O> {
        Type<O> type;
        private final String symbol;
        private final BiFunction<I, I, O> operator;

        @Override
        public String getSymbol() {
            return symbol;
        }

        @Override
        public O evaluate(I left, I right) {
            return operator.apply(left, right);
        }

        @Override
        public Type<O> getOutputType(Type<I> left, Type<I> right) {
            return type;
        }
    }
}
