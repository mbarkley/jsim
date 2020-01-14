package ca.mbarkley.jsim.model;

import java.util.List;
import java.util.Optional;

public abstract class BooleanExpression extends Expression<Boolean> {

    @Override
    public Type<Boolean> getType() {
        return Types.BOOLEAN_TYPE;
    }

    public static Constant<Boolean> TRUE = new Constant<>(Types.BOOLEAN_TYPE, true);
    public static Constant<Boolean> FALSE = new Constant<>(Types.BOOLEAN_TYPE, false);

    public static class BooleanOperators {
        public static final BinaryOperator<Boolean, Boolean> equality = BinaryOperator.equality();
        public static final BinaryOperator<Boolean, Boolean> and = BinaryOperator.create(Types.BOOLEAN_TYPE, "and", (l, r) -> l && r);
        public static final BinaryOperator<Boolean, Boolean> or = BinaryOperator.create(Types.BOOLEAN_TYPE, "or", (l, r) -> l || r);

        public static Optional<BinaryOperator<Boolean, Boolean>> lookup(String symbol) {
            return HasSymbol.lookup(symbol, List.of(equality, and, or));
        }
    }

    public static class IntegerComparisons {
        public static final BinaryOperator<Integer, Boolean> equality = BinaryOperator.equality();
        public static final BinaryOperator<Integer, Boolean> lessThan = BinaryOperator.create(Types.BOOLEAN_TYPE, "<", (l, r) -> l < r);
        public static final BinaryOperator<Integer, Boolean> greaterThan = BinaryOperator.create(Types.BOOLEAN_TYPE, ">", (l, r) -> l > r);

        public static Optional<BinaryOperator<Integer, Boolean>> lookup(String symbol) {
            return HasSymbol.lookup(symbol, List.of(equality, lessThan, greaterThan));
        }
    }
}
