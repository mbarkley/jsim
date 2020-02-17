package ca.mbarkley.jsim.eval;

import ca.mbarkley.jsim.model.Expression;
import lombok.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Value
public class LexicalScope {
    Map<String, Expression<?>> definitions;

    public LexicalScope with(String identifier, Expression<?> value) {
        final Map<String, Expression<?>> defCopy = new HashMap<>(definitions);
        defCopy.put(identifier, value);

        return new LexicalScope(Collections.unmodifiableMap(defCopy));
    }
}
