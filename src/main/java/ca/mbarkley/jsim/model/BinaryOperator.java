package ca.mbarkley.jsim.model;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface BinaryOperator<I, O> extends HasSymbol<BinaryOperator<I, O>> {
    O evaluate(I left, I right);
    Class<I> getType();

    static <T> BinaryOperator<T, Boolean> equality(Class<T> type) {
        return new Equality<>(type);
    }

    static <I, O> BinaryOperator<I, O> create(Class<I> inputType, String symbol, BiFunction<I, I, O> operator) {
        return new SimpleBinaryOperator<I, O>(inputType, symbol, operator);
    }

    @Value
    class Equality<T> implements BinaryOperator<T, Boolean> {
        private final Class<T> type;

        @Override
        public Boolean evaluate(T left, T right) {
            return Objects.equals(left, right);
        }

        @Override
        public Class<T> getType() {
            return type;
        }

        @Override
        public String getSymbol() {
            return "=";
        }
    }

    @Value
    @EqualsAndHashCode(exclude = "operator")
    class SimpleBinaryOperator<I, O> implements BinaryOperator<I, O> {
        private final Class<I> inputType;
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
        public Class<I> getType() {
            return inputType;
        }
    }
}
