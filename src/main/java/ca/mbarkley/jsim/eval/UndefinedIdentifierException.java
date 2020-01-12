package ca.mbarkley.jsim.eval;

import static java.lang.String.format;

public class UndefinedIdentifierException extends RuntimeException {
    public UndefinedIdentifierException(String identifier) {
        super(format("Undefined identifier [%s]", identifier));
    }
}
