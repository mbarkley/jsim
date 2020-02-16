package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.model.Expression.Constant;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import static java.util.stream.Collectors.toMap;

public class Vectors {
    public static Vector of(Map<Symbol, Constant<?>> components) {
        final Map<Symbol, ? extends Type<?>> componentTypes = components.entrySet()
                                                                        .stream()
                                                                        .map(entry -> Map.entry(entry.getKey(), entry.getValue().getType()))
                                                                        .collect(toMap(Entry::getKey, Entry::getValue));
        return new Vector(Types.vectorTypeOf(componentTypes), new TreeMap<>(components));
    }
}
