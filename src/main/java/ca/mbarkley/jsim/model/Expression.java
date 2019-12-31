package ca.mbarkley.jsim.model;

import lombok.Value;

import static java.lang.String.format;

public abstract class Expression extends Statement {
    private Expression() {}

    public abstract ExpressionType getType();

    @Value
    public static class Constant extends Expression {
        int value;

        @Override
        public ExpressionType getType() {
            return ExpressionType.CONSTANT;
        }

        @Override
        public String toString() {
            return "" + value;
        }
    }

    @Value
    public static class HomogeneousDicePool extends Expression {
        int numberOfDice;
        int diceSides;

        @Override
        public ExpressionType getType() {
            return ExpressionType.HOMOGENEOUS;
        }

        @Override
        public String toString() {
            return format("%dd%d", numberOfDice, diceSides);
        }
    }

    @Value
    public static class BinaryOpExpression extends Expression {
        Expression left;
        Operator operator;
        Expression right;

        @Override
        public ExpressionType getType() {
            return ExpressionType.BINARY;
        }

        @Override
        public String toString() {
            return format("%s %s %s", left, operator, right);
        }
    }

    public enum Operator {
        PLUS("+"), MINUS("-");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    public enum ExpressionType {
        CONSTANT, HOMOGENEOUS, BINARY;
    }
}
