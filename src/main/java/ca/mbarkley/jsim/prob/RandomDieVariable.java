package ca.mbarkley.jsim.prob;

import lombok.Value;

@Value
public class RandomDieVariable {
    int sides;

    @Override
    public String toString() {
        return "d" + sides;
    }
}
