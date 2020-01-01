package ca.mbarkley.jsim;

import ca.mbarkley.jsim.model.Expression;
import ca.mbarkley.jsim.model.Question;
import ca.mbarkley.jsim.prob.Event;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.stream.Stream;

import static ca.mbarkley.jsim.prob.Event.productOfIndependent;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
@Builder
public class Calculator {
    public Map<Boolean, Event<Boolean>> calculateResult(Question question) {
        final Stream<Event<Integer>> left = question.getLeft().events();
        final Stream<Event<Integer>> right = question.getRight().events();

        return productOfIndependent(left,
                                    right,
                                    (l, r) -> question.getComparator().evaluate(l, r))
                .collect(toMap(Event::getValue, identity()));
    }

    public Map<Integer, Event<Integer>> calculateResult(Expression expression) {
        return expression.events()
                         .collect(toMap(Event::getValue, identity()));
    }
}
