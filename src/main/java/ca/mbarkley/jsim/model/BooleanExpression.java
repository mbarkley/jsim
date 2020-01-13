package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.prob.Event;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static ca.mbarkley.jsim.prob.Event.productOfIndependent;
import static java.lang.String.format;

public abstract class BooleanExpression extends Expression<Boolean> {

    @Override
    public Type<Boolean> getType() {
        return Types.BOOLEAN_TYPE;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class BinaryOpBooleanExpression extends BooleanExpression {
        Expression<Boolean> left;
        BinaryOperator<Boolean, Boolean> operator;
        Expression<Boolean> right;

        @Override
        public Stream<Event<Boolean>> events() {
            final Stream<Event<Boolean>> left = getLeft().events();
            final Stream<Event<Boolean>> right = getRight().events();

            return productOfIndependent(left,
                                        right,
                                        operator::evaluate);
        }

        @Override
        public String toString() {
            return format("%s %s %s", left, operator.getSymbol(), right);
        }
    }

    public static Constant<Boolean> TRUE = new Constant<>(Types.BOOLEAN_TYPE, true);
    public static Constant<Boolean> FALSE = new Constant<>(Types.BOOLEAN_TYPE, false);

    public static class BooleanOperators {
        public static final BinaryOperator<Boolean, Boolean> equality = BinaryOperator.equality();
        public static final BinaryOperator<Boolean, Boolean> and = BinaryOperator.create("and", (l, r) -> l && r);
        public static final BinaryOperator<Boolean, Boolean> or = BinaryOperator.create("or", (l, r) -> l || r);

        public static Optional<BinaryOperator<Boolean, Boolean>> lookup(String symbol) {
            return HasSymbol.lookup(symbol, List.of(equality, and, or));
        }
    }

    public static class IntegerComparisons {
        public static final BinaryOperator<Integer, Boolean> equality = BinaryOperator.equality();
        public static final BinaryOperator<Integer, Boolean> lessThan = BinaryOperator.create("<", (l, r) -> l < r);
        public static final BinaryOperator<Integer, Boolean> greaterThan = BinaryOperator.create(">", (l, r) -> l > r);

        public static Optional<BinaryOperator<Integer, Boolean>> lookup(String symbol) {
            return HasSymbol.lookup(symbol, List.of(equality, lessThan, greaterThan));
        }
    }
}
