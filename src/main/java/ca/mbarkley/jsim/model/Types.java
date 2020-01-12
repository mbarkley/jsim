package ca.mbarkley.jsim.model;

public abstract class Types {
    public static final Type<Integer> INTEGER_TYPE = new Type.IntegerType();
    public static final Type<Boolean> BOOLEAN_TYPE = new Type.BooleanType();
    public static final Type<String> SYMBOL_TYPE = new Type.SymbolType();

    private Types() {}
}
