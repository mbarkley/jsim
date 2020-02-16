package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.model.Expression.Constant;

public class Constants {
    public static Constant<Integer> of(Integer value) {
        return new Constant<>(Types.INTEGER_TYPE, value);
    }

    public static Constant<Symbol> of(String value) {
        final Symbol symbol = new Symbol(Symbol.Mark.TICK, value);
        return new Constant<>(Types.symbolTypeOf(symbol), symbol);
    }

    public static Constant<Boolean> of(Boolean value) {
        return new Constant<>(Types.BOOLEAN_TYPE, value);
    }
}
