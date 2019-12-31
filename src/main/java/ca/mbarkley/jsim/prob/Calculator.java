package ca.mbarkley.jsim.prob;

import ca.mbarkley.jsim.model.Expression;
import ca.mbarkley.jsim.model.Question;
import com.codepoetics.protonpack.StreamUtils;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingByConcurrent;

public class Calculator {
    public double calculateProbability(Question question) {
        final Supplier<IntStream> leftValues = () -> values(question.getLeft());
        final Supplier<IntStream> rightValues = () -> values(question.getRight());
        final BiFunction<Integer, Integer, Boolean> combiner;
        switch (question.getComparator()) {
            case LT:
                combiner = (l, r) -> l < r;
                break;
            case GT:
                combiner = (l, r) -> l > r;
                break;
            case EQ:
                combiner = Integer::equals;
                break;
            default:
                throw new IllegalStateException();
        }
        final Stream<Boolean> answerStream = product(leftValues, rightValues, combiner);

        final ConcurrentMap<Boolean, Long> counts = answerStream.parallel()
                                                                .collect(groupingByConcurrent(identity(), counting()));

        final BigDecimal total = BigDecimal.valueOf(counts.getOrDefault(true, 0L) + counts.getOrDefault(false, 0L));
        final BigDecimal successes = BigDecimal.valueOf(counts.getOrDefault(true, 0L));

        final BigDecimal result = successes.divide(total, 10000, RoundingMode.HALF_EVEN);

        return result.doubleValue();
    }

    private IntStream values(Expression expression) {
        switch (expression.getType()) {
            case CONSTANT:
                Expression.Constant constant = (Expression.Constant) expression;
                return IntStream.of(constant.getValue());
            case HOMOGENEOUS:
                Expression.HomogeneousDicePool dicePool = (Expression.HomogeneousDicePool) expression;
                return StreamUtils.unfold(new DicePoolState(dicePool), DicePoolState::increment).mapToInt(state -> sum(state.diceValues));
            case BINARY:
                Expression.BinaryOpExpression binaryExpression = (Expression.BinaryOpExpression) expression;
                Supplier<IntStream> left = () -> values(binaryExpression.getLeft());
                Supplier<IntStream> right = () -> values(binaryExpression.getRight());
                BiFunction<Integer, Integer, Integer> combiner;
                switch (binaryExpression.getOperator()) {
                    case PLUS:
                        combiner = Integer::sum;
                        break;
                    case MINUS:
                        combiner = (l, r) -> l - r;
                        break;
                    default:
                        throw new IllegalStateException();
                }
                return product(left, right, combiner).mapToInt(n -> n);
            default:
                throw new IllegalStateException();
        }
    }

    private <V> Stream<V> product(Supplier<IntStream> left, Supplier<IntStream> right, BiFunction<Integer, Integer, V> combiner) {
        return left.get()
                   .boxed()
                   .flatMap(l -> {
                       final Stream<Integer> repeatedLeft = Stream.generate(() -> l);
                       return StreamUtils.zip(repeatedLeft, right.get().boxed(), combiner);
                   });
    }

    private int sum(int[] diceValues) {
        int val = 0;
        for (int diceValue : diceValues) {
            val += diceValue;
        }

        return val;
    }


    @Value
    @RequiredArgsConstructor
    private static class DicePoolState {
        Expression.HomogeneousDicePool dicePool;
        int[] diceValues;

        public DicePoolState(Expression.HomogeneousDicePool dicePool) {
            this.dicePool = dicePool;
            diceValues = new int[dicePool.getNumberOfDice()];
            Arrays.fill(diceValues, 1);
        }

        Optional<DicePoolState> increment() {
            final int[] curValues = getDiceValues();
            final int[] newValues = Arrays.copyOf(curValues, curValues.length);
            final int diceSides = getDicePool().getDiceSides();

            boolean valid = false;
            for (int i = 0; i < newValues.length; i++) {
                newValues[i] += 1;
                if (newValues[i] > diceSides) {
                    newValues[i] = 1;
                } else {
                    valid = true;
                    break;
                }
            }

            if (valid) {
                return Optional.of(new DicePoolState(dicePool, newValues));
            } else {
                return Optional.empty();
            }
        }
    }
}
