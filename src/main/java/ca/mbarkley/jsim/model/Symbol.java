package ca.mbarkley.jsim.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

import java.util.Arrays;

import static java.lang.String.format;

@Value
@EqualsAndHashCode
public class Symbol implements Comparable<Symbol> {
    @EqualsAndHashCode.Exclude
    Mark mark;
    String symbol;

    @Override
    public String toString() {
        return mark.getSymbol() + symbol;
    }

    @Override
    public int compareTo(Symbol o) {
        return symbol.compareTo(o.symbol);
    }

    public Type<Symbol> getType() {
        return new Type.SymbolType(this);
    }

    public static Symbol fromText(String rawText) {
        final Mark mark = Arrays.stream(Mark.values())
                                .filter(m -> rawText.startsWith(m.getSymbol()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(format("Invalid symbol text does not start with valid charcter [%s]",
                                                                                       String.join("", Arrays.stream(Mark.values())
                                                                                                             .map(Object::toString)
                                                                                                             .toArray(String[]::new)))));

        return new Symbol(mark, rawText.substring(mark.getSymbol().length()));
    }

    public enum Mark {
        TICK("'"), COLON(":");

        @Getter
        private final String symbol;

        Mark(String symbol) {
            this.symbol = symbol;
        }
    }

    public static Symbol of(String value) {
        return new Symbol(Mark.TICK, value);
    }
}
