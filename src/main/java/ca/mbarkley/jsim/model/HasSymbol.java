package ca.mbarkley.jsim.model;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

public interface HasSymbol<T> {

    /**
     * @return The string representation of this symbol.
     */
    String getSymbol();

    static <T extends HasSymbol<? super T>> Optional<T> lookup(String symbol, List<T> symbolSet) {
        for (var op : symbolSet) {
            if (op.getSymbol().equals(symbol)) {
                return Optional.of(op);
            }
        }

        return Optional.empty();
    }

    static <T extends HasSymbol<? super T>> Optional<T> lookup(String symbol, T[] symbolSet) {
        return lookup(symbol, asList(symbolSet));
    }
}
