package ca.mbarkley.jsim.model;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Objects;
import java.util.function.BiFunction;

public interface BinaryOperator<I, O> extends HasSymbol<BinaryOperator<I, O>> {
    O evaluate(I left, I right);

    static <T extends Comparable<T>> BinaryOperator<T, Boolean> equality() {
        return new Equality<>();
    }

    static <I, O> BinaryOperator<I, O> create(String symbol, BiFunction<I, I, O> operator) {
        return new SimpleBinaryOperator<I, O>(symbol, operator);
    }

    @Value
    class Equality<T extends Comparable<T>> implements BinaryOperator<T, Boolean> {
        @Override
        public Boolean evaluate(T left, T right) {
            return Objects.equals(left, right);
        }

        @Override
        public String getSymbol() {
            return "=";
        }
    }

    @Value
    @EqualsAndHashCode(exclude = "operator")
    class SimpleBinaryOperator<I, O> implements BinaryOperator<I, O> {
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
    }
}
