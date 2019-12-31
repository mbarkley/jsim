package ca.mbarkley.jsim;

import ca.mbarkley.jsim.model.Question;
import ca.mbarkley.jsim.prob.Event;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static ca.mbarkley.jsim.prob.Event.productOfIndependent;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
@Builder
public class Calculator {
    private final int scale;

    public double calculateProbability(Question question) {
        final Stream<Event<Integer>> left = question.getLeft().events(scale);
        final Stream<Event<Integer>> right = question.getRight().events(scale);
        final List<Event<Boolean>> results = productOfIndependent(left,
                                                                  right,
                                                                  (l, r) -> question.getComparator().evaluate(l, r)).collect(toList());

        return results.stream()
                      .filter(Event::getValue)
                      .map(Event::getProbability)
                      .findFirst()
                      .orElse(BigDecimal.ZERO)
                      .doubleValue();
    }
}
