package ca.mbarkley.jsim.eval;

import ca.mbarkley.jsim.model.Expression;
import lombok.Value;

import java.util.Map;

@Value
public class Context {
    Map<String, Expression<?>> definitions;
}
