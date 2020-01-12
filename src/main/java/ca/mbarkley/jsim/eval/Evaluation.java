package ca.mbarkley.jsim.eval;

import ca.mbarkley.jsim.model.Expression;
import lombok.Value;

import java.util.List;

@Value
public class Evaluation {
    Context context;
    List<Expression<?>> expressions;
}
