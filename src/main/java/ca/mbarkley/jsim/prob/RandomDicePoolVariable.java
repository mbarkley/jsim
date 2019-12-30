package ca.mbarkley.jsim.prob;

import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Value
public class RandomDicePoolVariable {
    List<RandomDieVariable> dice;
    int modifier;

    @Override
    public String toString() {
        final Map<RandomDieVariable, Long> diceCounts = dice.stream()
                                                            .collect(groupingBy(Function.identity(), Collectors.counting()));

        final String diceString = diceCounts.entrySet()
                                            .stream()
                                            .map(e -> String.format("%d%s", e.getValue(), e.getKey()))
                                            .collect(Collectors.joining(" + "));

        if (modifier == 0) {
            return diceString;
        } else if (modifier > 0) {
            return String.format("%s + %d", diceString, modifier);
        } else {
            return String.format("%s - %d", diceString, Math.abs(modifier));
        }
    }
}
