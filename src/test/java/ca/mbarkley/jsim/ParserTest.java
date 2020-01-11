package ca.mbarkley.jsim;

import ca.mbarkley.jsim.antlr.JSimLexer;
import ca.mbarkley.jsim.model.Expression;
import ca.mbarkley.jsim.model.Expression.*;
import ca.mbarkley.jsim.model.Question;
import ca.mbarkley.jsim.model.Statement;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static ca.mbarkley.jsim.model.Expression.Operator.PLUS;
import static ca.mbarkley.jsim.model.Expression.Operator.TIMES;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ParserTest {
    Parser parser = new Parser();

    @Test
    public void singleDieRoll() {
        final String expression = "d6";

        final List<Statement<?>> result = parser.parse(expression);

        assertThat(result).containsExactly(new HomogeneousDicePool(1, 6));
    }

    @Test
    public void multipleDiceRoll() {
        final String expression = "3d8";

        final List<Statement<?>> result = parser.parse(expression);

        assertThat(result).containsExactly(new HomogeneousDicePool(3, 8));
    }

    @Test
    public void highDieRoll() {
        final String expression = "3d6H2";

        final List<Statement<?>> result = parser.parse(expression);

        assertThat(result).containsExactly(new Expression.HighDice(new HomogeneousDicePool(3, 6), 2));
    }

    @Test
    public void lowDieRoll() {
        final String expression = "3d6L2";

        final List<Statement<?>> result = parser.parse(expression);

        assertThat(result).containsExactly(new Expression.LowDice(new HomogeneousDicePool(3, 6), 2));
    }

    @Test
    public void leadingWhitespace() {
        final String expression = " d6";

        final List<Statement<?>> result = parser.parse(expression);

        assertThat(result).containsExactly(new HomogeneousDicePool(1, 6));
    }

    @Test
    public void trailingWhitespace() {
        final String expression = "d6 ";

        final List<Statement<?>> result = parser.parse(expression);

        assertThat(result).containsExactly(new HomogeneousDicePool(1, 6));
    }

    @Test
    public void singleRollPlusConstant() {
        final String expression = "d6 + 1";

        final List<Statement<?>> result = parser.parse(expression);

        assertThat(result).containsExactly(new BinaryOpExpression(new HomogeneousDicePool(1, 6), PLUS, new Constant(1)));
    }

    @Test
    public void singleRollMinusConstant() {
        final String expression = "d6 - 1";

        final List<Statement<?>> result = parser.parse(expression);

        assertThat(result).containsExactly(new BinaryOpExpression(new HomogeneousDicePool(1, 6), Operator.MINUS, new Constant(1)));
    }

    @Test
    public void multipleDiceRollPlusConstant() {
        final String expression = "3d8 + 1";

        final List<Statement<?>> result = parser.parse(expression);

        assertThat(result).containsExactly(new BinaryOpExpression(new HomogeneousDicePool(3, 8), PLUS, new Constant(1)));
    }

    @Test
    public void multipleDiceRollMinusConstant() {
        final String expression = "3d8 - 1";

        final List<Statement<?>> result = parser.parse(expression);

        assertThat(result).containsExactly(new BinaryOpExpression(new HomogeneousDicePool(3, 8), Operator.MINUS, new Constant(1)));
    }

    @Test
    public void multipleDiceRollTimesConstant() {
        final String expression = "3d8 * 2";

        final List<Statement<?>> result = parser.parse(expression);

        assertThat(result).containsExactly(new BinaryOpExpression(new HomogeneousDicePool(3, 8), Operator.TIMES, new Constant(2)));
    }

    @Test
    public void multipleDiceRollDivideConstant() {
        final String expression = "3d8 / 2";

        final List<Statement<?>> result = parser.parse(expression);

        assertThat(result).containsExactly(new BinaryOpExpression(new HomogeneousDicePool(3, 8), Operator.DIVIDE, new Constant(2)));
    }

    @Test
    public void sumOfDicePools() {
        final String expression = "3d8 + 2d6 + 1";

        final List<Statement<?>> result = parser.parse(expression);

        final BinaryOpExpression expected = new BinaryOpExpression(
                new BinaryOpExpression(new HomogeneousDicePool(3, 8), PLUS, new HomogeneousDicePool(2, 6)),
                PLUS,
                new Constant(1));
        assertThat(result).containsExactly(expected);
    }

    @Test
    public void lessThan() {
        final String expression = "3d8 < 2d6";

        final List<Statement<?>> result = parser.parse(expression);

        final Expression left = new HomogeneousDicePool(3, 8);
        final Expression right = new HomogeneousDicePool(2, 6);
        final Question expected = new Question.BinaryBooleanExpression(left, Question.Comparator.LT, right);
        assertThat(result).containsExactly(expected);
    }

    @Test
    public void greaterThan() {
        final String expression = "3d8 > 2d6";

        final List<Statement<?>> result = parser.parse(expression);

        final Expression left = new HomogeneousDicePool(3, 8);
        final Expression right = new HomogeneousDicePool(2, 6);
        final Question expected = new Question.BinaryBooleanExpression(left, Question.Comparator.GT, right);
        assertThat(result).containsExactly(expected);
    }

    @Test
    public void equalQuestion() {
        final String expression = "3d8 = 2d6";

        final List<Statement<?>> result = parser.parse(expression);

        final Expression left = new HomogeneousDicePool(3, 8);
        final Expression right = new HomogeneousDicePool(2, 6);
        final Question expected = new Question.BinaryBooleanExpression(left, Question.Comparator.EQ, right);
        assertThat(result).containsExactly(expected);
    }

    @Test
    public void subtractionThenAddition() {
        final String expression = "2 - 1 + 1";

        final List<Statement<?>> result = parser.parse(expression);

        final Expression expected = new BinaryOpExpression(
                new BinaryOpExpression(new Constant(2), Operator.MINUS, new Constant(1)),
                PLUS,
                new Constant(1)
        );
        assertThat(result).containsExactly(expected);
    }

    @Test
    public void bracketed() {
        final String expression = "2 * (2d6 + 1)";

        final List<Statement<?>> result = parser.parse(expression);

        final Expression expected = new BinaryOpExpression(
                new Constant(2),
                TIMES,
                new Bracketed(new BinaryOpExpression(
                        new HomogeneousDicePool(2, 6),
                        PLUS,
                        new Constant(1)
                ))
        );
        assertThat(result).containsExactly(expected);
    }

    @Test
    public void complexConstantExpression() {
        final String expression = "3 * (3 + 1) / 4 * 10";

        final List<Statement<?>> result = parser.parse(expression);

        assertThat(result).hasToString(List.of(expression).toString());
    }

    @Test
    public void invalidQuestionCausesExceptionWithCharacterInfo() {
        final String expression = "2d6 > ";

        try {
            final List<Statement<?>> result = parser.parse(expression);
            fail(format("Parsed a value: %s", result));
        } catch (RecognitionException re) {
            assertThat(re.getOffendingToken()).extracting(Token::getLine)
                                              .isEqualTo(1);
            assertThat(re.getOffendingToken()).extracting(Token::getCharPositionInLine)
                                              .isEqualTo(6);
        }
    }

    @Test
    public void invalidExpressionInQuestionCausesExceptionWithCharacterInfo() {
        final String expression = "2d6 > 3 * ";

        try {
            final List<Statement<?>> result = parser.parse(expression);
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
            final List<Statement<?>> result = parser.parse(expression);
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
            final List<Statement<?>> result = parser.parse(expression);
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
            final List<Statement<?>> result = parser.parse(expression);
            fail(format("Parsed a value: %s", result));
        } catch (RecognitionException re) {
            assertThat(re.getOffendingToken()).extracting(Token::getLine)
                                              .isEqualTo(3);
            assertThat(re.getOffendingToken()).extracting(Token::getCharPositionInLine)
                                              .isEqualTo(6);
        }
    }

    @Test
    public void invalidEmptyInput() {
        final String expression = "";

        try {
            final List<Statement<?>> result = parser.parse(expression);
            fail(format("Parsed a value: %s", result));
        } catch (RecognitionException re) {
            assertThat(re.getOffendingToken()).extracting(Token::getLine)
                                              .isEqualTo(1);
            assertThat(re.getOffendingToken()).extracting(Token::getCharPositionInLine)
                                              .isEqualTo(0);
        }
    }

    @Test
    public void invalidToken() {
        final String expression = "2d6 + abc";

        try {
            final List<Statement<?>> result = parser.parse(expression);
            fail(format("Parsed a value: %s", result));
        } catch (RecognitionException re) {
            assertThat(re.getOffendingToken()).extracting(Token::getLine)
                                              .isEqualTo(1);
            assertThat(re.getOffendingToken()).extracting(Token::getCharPositionInLine)
                                              .isEqualTo(9);
            assertThat(re.getOffendingToken()).extracting(Token::getStartIndex)
                                              .isEqualTo(9);
            assertThat(re.getOffendingToken()).extracting(Token::getStopIndex)
                                              .isEqualTo(8);
        }
    }

    @Test
    public void wrongToken() {
        final String expression = "2d6 d8";

        try {
            final List<Statement<?>> result = parser.parse(expression);
            fail(format("Parsed a value: %s", result));
        } catch (RecognitionException re) {
            assertThat(re.getOffendingToken()).extracting(Token::getLine)
                                              .isEqualTo(1);
            assertThat(re.getOffendingToken()).extracting(Token::getCharPositionInLine)
                                              .isEqualTo(4);
            assertThat(re.getOffendingToken()).extracting(Token::getStartIndex)
                                              .isEqualTo(4);
            assertThat(re.getOffendingToken()).extracting(Token::getStopIndex)
                                              .isEqualTo(4);
        }
    }
}
