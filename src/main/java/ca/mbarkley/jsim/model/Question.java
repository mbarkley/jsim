package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.prob.Event;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.stream.Stream;

import static ca.mbarkley.jsim.prob.Event.productOfIndependent;
import static java.lang.String.format;

@Value
@EqualsAndHashCode(callSuper = false)
public class Question extends Statement<Boolean> {
    Expression left;
    Comparator comparator;
    Expression right;

    @Override
    public Stream<Event<Boolean>> events() {
        final Stream<Event<Integer>> left = getLeft().events();
        final Stream<Event<Integer>> right = getRight().events();

        return productOfIndependent(left,
                                    right,
                                    (l, r) -> getComparator().evaluate(l, r));
    }

    @Override
    public String toString() {
        return format("%s %s %s", left, comparator, right);
    }

    public enum Comparator {
        LT("<") {
            @Override
            public boolean evaluate(int left, int right) {
                return left < right;
            }
        }, GT(">") {
            @Override
            public boolean evaluate(int left, int right) {
                return left > right;
            }
        }, EQ("=") {
            @Override
            public boolean evaluate(int left, int right) {
                return left == right;
            }
        };

        private final String symbol;

        Comparator(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }

        public abstract boolean evaluate(int left, int right);
    }
}
