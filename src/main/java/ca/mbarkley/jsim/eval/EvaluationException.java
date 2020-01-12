package ca.mbarkley.jsim.eval;

import ca.mbarkley.jsim.model.Expression;

import java.util.Set;

import static java.lang.String.format;

public class EvaluationException extends RuntimeException {
    public EvaluationException() {
    }

    public EvaluationException(String message) {
        super(message);
    }

    public EvaluationException(String message, Throwable cause) {
        super(message, cause);
    }

    public EvaluationException(Throwable cause) {
        super(cause);
    }

    public static class UndefinedIdentifierException extends EvaluationException {
        public UndefinedIdentifierException(String identifier) {
            super(format("Undefined identifier [%s]", identifier));
        }
    }

    public static class DiceTypeException extends EvaluationException {
        public DiceTypeException(Set<String> typeNames) {
            super(format("Dice sides in declaration must have common type, but these types were found [%s]", typeNames));
        }
    }

    public static class EqualityTypeException extends EvaluationException {
        public EqualityTypeException(Expression<?> left, Expression<?> right) {
            super(format("Left [%s] and right [%s] sides of equality have different types [%s] and [%s]", left, right, left.getType(), right.getType()));
        }
    }
}
