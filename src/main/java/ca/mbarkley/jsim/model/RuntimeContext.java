package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.model.Expression.Constant;
import lombok.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Value
class RuntimeContext {
    Map<String, Constant<?>> definitions;

    public RuntimeContext with(String identifier, Constant<?> value) {
        final Map<String, Constant<?>> defCopy = new HashMap<>(definitions);
        defCopy.put(identifier, value);

        return new RuntimeContext(Collections.unmodifiableMap(defCopy));
    }
}
