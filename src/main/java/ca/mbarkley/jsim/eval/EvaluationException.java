package ca.mbarkley.jsim.eval;

import ca.mbarkley.jsim.antlr.JSimParser;
import ca.mbarkley.jsim.model.Expression;
import ca.mbarkley.jsim.model.Type;
import ca.mbarkley.jsim.model.Type.VectorType;

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

    public static class DuplicateDimensionDeclarationException extends EvaluationException {
        public DuplicateDimensionDeclarationException(JSimParser.VectorLiteralContext ctx, String duplicateName) {
            super(format("Dimension [%s] declared twice in vector [%s]", duplicateName, ctx.getText()));
        }
    }

    public static class UnmergableVectorTypeException extends EvaluationException {
        public UnmergableVectorTypeException(String message) {
            super(message);
        }
    }

    public static class InvalidTypeException extends EvaluationException {
        public InvalidTypeException(Type<?> expected, Type<?> observed) {
            super(format("Expected type [%s] but observed [%s]", expected.getName(), observed.getName()));
        }

        public InvalidTypeException(String message) {
            super(message);
        }
    }

    public static class UnknownOperatorException extends EvaluationException {
        public UnknownOperatorException(Type<?> left, String symbol, Type<?> right) {
            super(format("Unknown operator [%s] for types [%s] and [%s]", symbol, left.getName(), right.getName()));
        }
    }
}
