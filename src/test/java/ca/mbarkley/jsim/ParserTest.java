package ca.mbarkley.jsim;

import ca.mbarkley.jsim.eval.Parser;
import ca.mbarkley.jsim.model.*;
import ca.mbarkley.jsim.model.BooleanExpression.IntegerComparisons;
import ca.mbarkley.jsim.model.Expression.BinaryOpExpression;
import ca.mbarkley.jsim.model.Expression.Bracketed;
import ca.mbarkley.jsim.model.Expression.Constant;
import ca.mbarkley.jsim.model.IntegerExpression.HighDice;
import ca.mbarkley.jsim.model.IntegerExpression.HomogeneousDicePool;
import ca.mbarkley.jsim.model.IntegerExpression.LowDice;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.junit.Test;

import java.util.List;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ParserTest {
    public static final BinaryOperator<Integer, Integer> PLUS = BinaryOperators.intAddition;
    public static final BinaryOperator<Integer, Integer> MINUS = BinaryOperators.intSubtraction;
    private static final BinaryOperator<Integer, Integer> TIMES = BinaryOperators.multiplication;
    private static final BinaryOperator<Integer, Integer> DIVIDE = BinaryOperators.division;

    Parser parser = new Parser();

    @Test
    public void singleDieRoll() {
        final String expression = "d6";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        assertThat(result).containsExactly(new HomogeneousDicePool(1, 6));
    }

    @Test
    public void multipleDiceRoll() {
        final String expression = "3d8";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        assertThat(result).containsExactly(new HomogeneousDicePool(3, 8));
    }

    @Test
    public void highDieRoll() {
        final String expression = "3d6H2";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        assertThat(result).containsExactly(new HighDice(new HomogeneousDicePool(3, 6), 2));
    }

    @Test
    public void lowDieRoll() {
        final String expression = "3d6L2";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        assertThat(result).containsExactly(new LowDice(new HomogeneousDicePool(3, 6), 2));
    }

    @Test
    public void leadingWhitespace() {
        final String expression = " d6";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        assertThat(result).containsExactly(new HomogeneousDicePool(1, 6));
    }

    @Test
    public void trailingWhitespace() {
        final String expression = "d6 ";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        assertThat(result).containsExactly(new HomogeneousDicePool(1, 6));
    }

    @Test
    public void singleRollPlusConstant() {
        final String expression = "d6 + 1";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        assertThat(result).containsExactly(new BinaryOpExpression<>(new HomogeneousDicePool(1, 6), PLUS, new Constant<>(Types.INTEGER_TYPE, 1)));
    }

    @Test
    public void singleRollMinusConstant() {
        final String expression = "d6 - 1";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        assertThat(result).containsExactly(new BinaryOpExpression<>(new HomogeneousDicePool(1, 6), MINUS, new Constant<>(Types.INTEGER_TYPE, 1)));
    }

    @Test
    public void multipleDiceRollPlusConstant() {
        final String expression = "3d8 + 1";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        assertThat(result).containsExactly(new BinaryOpExpression<>(new HomogeneousDicePool(3, 8), PLUS, new Constant<>(Types.INTEGER_TYPE, 1)));
    }

    @Test
    public void multipleDiceRollMinusConstant() {
        final String expression = "3d8 - 1";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        assertThat(result).containsExactly(new BinaryOpExpression<>(new HomogeneousDicePool(3, 8), MINUS, new Constant<>(Types.INTEGER_TYPE, 1)));
    }

    @Test
    public void multipleDiceRollTimesConstant() {
        final String expression = "3d8 * 2";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        assertThat(result).containsExactly(new BinaryOpExpression<>(new HomogeneousDicePool(3, 8), TIMES, new Constant<>(Types.INTEGER_TYPE, 2)));
    }

    @Test
    public void multipleDiceRollDivideConstant() {
        final String expression = "3d8 / 2";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        assertThat(result).containsExactly(new BinaryOpExpression<>(new HomogeneousDicePool(3, 8), DIVIDE, new Constant<>(Types.INTEGER_TYPE, 2)));
    }

    @Test
    public void sumOfDicePools() {
        final String expression = "3d8 + 2d6 + 1";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        final BinaryOpExpression expected = new BinaryOpExpression<>(
                new BinaryOpExpression<>(new HomogeneousDicePool(3, 8), PLUS, new HomogeneousDicePool(2, 6)),
                PLUS,
                new Constant<>(Types.INTEGER_TYPE, 1));
        assertThat(result).containsExactly(expected);
    }

    @Test
    public void lessThan() {
        final String expression = "3d8 < 2d6";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        final IntegerExpression left = new HomogeneousDicePool(3, 8);
        final IntegerExpression right = new HomogeneousDicePool(2, 6);
        final Expression<Boolean> expected = new BinaryOpExpression<>(left, IntegerComparisons.lessThan, right);
        assertThat(result).containsExactly(expected);
    }

    @Test
    public void greaterThan() {
        final String expression = "3d8 > 2d6";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        final IntegerExpression left = new HomogeneousDicePool(3, 8);
        final IntegerExpression right = new HomogeneousDicePool(2, 6);
        final Expression<Boolean> expected = new BinaryOpExpression<>(left, IntegerComparisons.greaterThan, right);
        assertThat(result).containsExactly(expected);
    }

    @Test
    public void equalQuestion() {
        final String expression = "3d8 = 2d6";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        final IntegerExpression left = new HomogeneousDicePool(3, 8);
        final IntegerExpression right = new HomogeneousDicePool(2, 6);
        final Expression<Boolean> expected = new BinaryOpExpression<>(left, BinaryOperator.strictEquality(), right);
        assertThat(result).containsExactly(expected);
    }

    @Test
    public void subtractionThenAddition() {
        final String expression = "2 - 1 + 1";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        final Expression<Integer> expected = new BinaryOpExpression<>(
                new BinaryOpExpression<>(new Constant<>(Types.INTEGER_TYPE, 2), MINUS, new Constant<>(Types.INTEGER_TYPE, 1)),
                PLUS,
                new Constant<>(Types.INTEGER_TYPE, 1)
        );
        assertThat(result).containsExactly(expected);
    }

    @Test
    public void bracketed() {
        final String expression = "2 * (2d6 + 1)";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        final Expression<Integer> expected = new BinaryOpExpression<>(
                new Constant<>(Types.INTEGER_TYPE, 2),
                TIMES,
                new Bracketed<>(new BinaryOpExpression<>(
                        new HomogeneousDicePool(2, 6),
                        PLUS,
                        new Constant<>(Types.INTEGER_TYPE, 1)
                ))
        );
        assertThat(result).containsExactly(expected);
    }

    @Test
    public void complexConstantExpression() {
        final String expression = "3 * (3 + 1) / 4 * 10";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();

        assertThat(result).hasToString(List.of(expression).toString());
    }

    @Test
    public void invalidQuestionCausesExceptionWithCharacterInfo() {
        final String expression = "2d6 > ";

        try {
            final List<Expression<?>> result = parser.parse(expression).getExpressions();
            fail(format("Parsed a value: %s", result));
        } catch (RecognitionException re) {
            assertThat(re.getOffendingToken()).extracting(Token::getLine)
                                              .isNotEqualTo(0);
            assertThat(re.getOffendingToken()).extracting(Token::getCharPositionInLine)
                                              .isNotEqualTo(0);
        } catch (Exception e) {
            throw new AssertionError("Wrong exception type", e);
        }
    }

    @Test
    public void invalidExpressionInQuestionCausesExceptionWithCharacterInfo() {
        final String expression = "2d6 > 3 * ";

        try {
            final List<Expression<?>> result = parser.parse(expression).getExpressions();
            fail(format("Parsed a value: %s", result));
        } catch (RecognitionException re) {
            assertThat(re.getOffendingToken()).extracting(Token::getLine)
                                              .isEqualTo(1);
            assertThat(re.getOffendingToken()).extracting(Token::getCharPositionInLine)
                                              .isEqualTo(10);
        }
    }

    @Test
    public void invalidSubExpressionCausesExceptionWithCharacterInfo() {
        final String expression = "1 * ((3 + ))";

        try {
            final List<Expression<?>> result = parser.parse(expression).getExpressions();
            fail(format("Parsed a value: %s", result));
        } catch (RecognitionException re) {
            assertThat(re.getOffendingToken()).extracting(Token::getLine)
                                              .isEqualTo(1);
            assertThat(re.getOffendingToken()).extracting(Token::getCharPositionInLine)
                                              .isEqualTo(10);
        }
    }

    @Test
    public void invalidExpressionCausesExceptionWithCharacterInfo() {
        final String expression = "2d6 + ";

        try {
            final List<Expression<?>> result = parser.parse(expression).getExpressions();
            fail(format("Parsed a value: %s", result));
        } catch (RecognitionException re) {
            assertThat(re.getOffendingToken()).extracting(Token::getLine)
                                              .isEqualTo(1);
            assertThat(re.getOffendingToken()).extracting(Token::getCharPositionInLine)
                                              .isEqualTo(6);
        }
    }

    @Test
    public void invalidMultilineExpressionCausesExceptionWithCharacterInfo() {
        final String expression = "\t\n  \n2d6 + ";

        try {
            final List<Expression<?>> result = parser.parse(expression).getExpressions();
            fail(format("Parsed a value: %s", result));
        } catch (RecognitionException re) {
            assertThat(re.getOffendingToken()).extracting(Token::getLine)
                                              .isEqualTo(3);
            assertThat(re.getOffendingToken()).extracting(Token::getCharPositionInLine)
                                              .isEqualTo(6);
        }
    }

    @Test
    public void emptyInput() {
        final String expression = "";

        final List<Expression<?>> result = parser.parse(expression).getExpressions();
        assertThat(result).isEmpty();
    }

    @Test
    public void wrongToken() {
        final String expression = "2d6 d8";

        try {
            final List<Expression<?>> result = parser.parse(expression).getExpressions();
            fail(format("Parsed a value: %s", result));
        } catch (RecognitionException re) {
            assertThat(re.getOffendingToken()).extracting(Token::getLine)
                                              .isEqualTo(1);
            assertThat(re.getOffendingToken()).extracting(Token::getCharPositionInLine)
                                              .isEqualTo(4);
            assertThat(re.getOffendingToken()).extracting(Token::getStartIndex)
                                              .isEqualTo(4);
            assertThat(re.getOffendingToken()).extracting(Token::getStopIndex)
                                              .isEqualTo(5);
        }
    }
}
