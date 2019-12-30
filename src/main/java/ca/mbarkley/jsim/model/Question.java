package ca.mbarkley.jsim.model;

import lombok.Value;

import static java.lang.String.format;

@Value
public class Question extends Statement {
    Expression left;
    Comparator comparator;
    Expression right;

    @Override
    public String toString() {
        return format("%s %s %s", left, comparator, right);
    }

    public enum Comparator {
        LT("<"), GT(">"), EQ("=");

        private final String symbol;

        Comparator(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }
}
