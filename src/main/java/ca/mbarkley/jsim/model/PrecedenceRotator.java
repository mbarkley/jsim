package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.model.BooleanExpression.BinaryOpBooleanExpression;
import ca.mbarkley.jsim.model.BooleanExpression.BooleanOperator;
import ca.mbarkley.jsim.model.IntegerExpression.BinaryOpExpression;
import ca.mbarkley.jsim.model.IntegerExpression.Operator;

import java.util.Comparator;
import java.util.Optional;

/**
 * @param <O> Type of operator in binary statement type {@code R}
 * @param <S> Common super-type of binary statement components (ex. {@link IntegerExpression}, {@link BooleanExpression})
 * @param <R> A sub-type of {@code S} that is a binary statement combining two statements of type {@code S}
 *           with an operator of type {@code O}.
 */
public interface PrecedenceRotator<O, S extends Expression<?>, R extends S> extends Comparator<O> {
    Optional<R> asRotatable(S stmt);
    S left(R stmt);
    S right(R stmt);
    O operator(R stmt);
    R recombine(S left, O op, S right);

    /**
     * Rotates this statement if the following conditions hold:
     * <ol>
     * <li>The input statement is a binary statement of type {@code R}
     * <li>The right child of the input statement is also a binary statement of type {@code R}
     * <li>The operator of the input is less-than or equal to the operator of the right child,
     * using the natural ordering.
     * </ol>
     *
     * Rotating turns the first tree into the second.
     *
     *               +                 -
     *              / \               / \
     *             1  -     --->     +  3
     *               / \            / \
     *              2  3           1  2
     *
     * @param stmt A stmt that may or may not be of type {@code R}. Must not be null.
     * @return An optional with a rotated statement, or else an empty optional if not all conditions were met.
     */
    default Optional<S> maybeRotate(S stmt) {
        return asRotatable(stmt).flatMap(top -> asRotatable(right(top)).flatMap(right -> maybeRotate(top, right)));
    }

    private Optional<R> maybeRotate(R top, R right) {
        final S left = left(top);
        final O operator = operator(top);
        final S subleft = left(right);
        final S subright = right(right);
        final O suboperator = operator(right);
        if (compare(operator, suboperator) >= 0) {
            final R newLeft = recombine(left, operator, subleft);
            final R recombine = recombine(newLeft, suboperator, subright);
            return Optional.of(recombine);
        } else {
            return Optional.empty();
        }
    }

    static PrecedenceRotator<Operator, IntegerExpression, BinaryOpExpression> binaryOpExpressionRotator() {
        return new BinaryOpExpressionRotator();
    }

    static PrecedenceRotator<BooleanOperator, BooleanExpression, BinaryOpBooleanExpression> binaryOpQuestionRotator() {
        return new BinaryOpQuestionRotator();
    }
}

class BinaryOpExpressionRotator implements PrecedenceRotator<Operator, IntegerExpression, BinaryOpExpression> {

    @Override
    public Optional<BinaryOpExpression> asRotatable(IntegerExpression stmt) {
        if (stmt instanceof BinaryOpExpression) {
            return Optional.of((BinaryOpExpression) stmt);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public IntegerExpression left(BinaryOpExpression stmt) {
        return stmt.getLeft();
    }

    @Override
    public IntegerExpression right(BinaryOpExpression stmt) {
        return stmt.getRight();
    }

    @Override
    public Operator operator(BinaryOpExpression stmt) {
        return stmt.getOperator();
    }

    @Override
    public BinaryOpExpression recombine(IntegerExpression left, Operator op, IntegerExpression right) {
        return new BinaryOpExpression(left, op, right);
    }

    @Override
    public int compare(Operator o1, Operator o2) {
        return o1.getPrecedent() - o2.getPrecedent();
    }
}

class BinaryOpQuestionRotator implements PrecedenceRotator<BooleanOperator, BooleanExpression, BinaryOpBooleanExpression> {
    @Override
    public Optional<BinaryOpBooleanExpression> asRotatable(BooleanExpression stmt) {
        if (stmt instanceof BinaryOpBooleanExpression) {
            return Optional.of((BinaryOpBooleanExpression) stmt);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public BooleanExpression left(BinaryOpBooleanExpression stmt) {
        return stmt.getLeft();
    }

    @Override
    public BooleanExpression right(BinaryOpBooleanExpression stmt) {
        return stmt.getRight();
    }

    @Override
    public BooleanOperator operator(BinaryOpBooleanExpression stmt) {
        return stmt.getOperator();
    }

    @Override
    public BinaryOpBooleanExpression recombine(BooleanExpression left, BooleanOperator op, BooleanExpression right) {
        return new BinaryOpBooleanExpression(left, op, right);
    }

    @Override
    public int compare(BooleanOperator o1, BooleanOperator o2) {
        return o1.getPrecedent() - o2.getPrecedent();
    }
}
