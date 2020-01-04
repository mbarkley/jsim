package ca.mbarkley.jsim.model;

import java.util.Optional;

public interface HasSymbol<T> {

    /**
     * @return The string representation of this symbol.
     */
    String getSymbol();

    static <T extends HasSymbol<T>> Optional<T> lookup(String symbol, T[] symbolSet) {
        for (var op : symbolSet) {
            if (op.getSymbol().equals(symbol)) {
                return Optional.of(op);
            }
        }

        return Optional.empty();
    }
}
