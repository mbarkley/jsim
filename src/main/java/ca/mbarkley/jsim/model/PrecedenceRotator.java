package ca.mbarkley.jsim.model;

import java.util.Comparator;
import java.util.Optional;

/**
 * @param <O> Type of operator in binary statement type {@code R}
 * @param <S> Common super-type of binary statement components (ex. {@link Expression}, {@link Question})
 * @param <R> A sub-type of {@code S} that is a binary statement combining two statements of type {@code S}
 *           with an operator of type {@code O}.
 */
public interface PrecedenceRotator<O, S extends Statement<?>, R extends S> extends Comparator<O> {
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

    static PrecedenceRotator<Expression.Operator, Expression, Expression.BinaryOpExpression> binaryOpExpressionRotator() {
        return new BinaryOpExpressionRotator();
    }

    static PrecedenceRotator<Question.BooleanOperator, Question, Question.BinaryOpQuestion> binaryOpQuestionRotator() {
        return new BinaryOpQuestionRotator();
    }
}

class BinaryOpExpressionRotator implements PrecedenceRotator<Expression.Operator, Expression, Expression.BinaryOpExpression> {

    @Override
    public Optional<Expression.BinaryOpExpression> asRotatable(Expression stmt) {
        if (stmt instanceof Expression.BinaryOpExpression) {
            return Optional.of((Expression.BinaryOpExpression) stmt);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Expression left(Expression.BinaryOpExpression stmt) {
        return stmt.getLeft();
    }

    @Override
    public Expression right(Expression.BinaryOpExpression stmt) {
        return stmt.getRight();
    }

    @Override
    public Expression.Operator operator(Expression.BinaryOpExpression stmt) {
        return stmt.getOperator();
    }

    @Override
    public Expression.BinaryOpExpression recombine(Expression left, Expression.Operator op, Expression right) {
        return new Expression.BinaryOpExpression(left, op, right);
    }

    @Override
    public int compare(Expression.Operator o1, Expression.Operator o2) {
        return o1.getPrecedent() - o2.getPrecedent();
    }
}

class BinaryOpQuestionRotator implements PrecedenceRotator<Question.BooleanOperator, Question, Question.BinaryOpQuestion> {
    @Override
    public Optional<Question.BinaryOpQuestion> asRotatable(Question stmt) {
        if (stmt instanceof Question.BinaryOpQuestion) {
            return Optional.of((Question.BinaryOpQuestion) stmt);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Question left(Question.BinaryOpQuestion stmt) {
        return stmt.getLeft();
    }

    @Override
    public Question right(Question.BinaryOpQuestion stmt) {
        return stmt.getRight();
    }

    @Override
    public Question.BooleanOperator operator(Question.BinaryOpQuestion stmt) {
        return stmt.getOperator();
    }

    @Override
    public Question.BinaryOpQuestion recombine(Question left, Question.BooleanOperator op, Question right) {
        return new Question.BinaryOpQuestion(left, op, right);
    }

    @Override
    public int compare(Question.BooleanOperator o1, Question.BooleanOperator o2) {
        return o1.getPrecedent() - o2.getPrecedent();
    }
}
