package ca.mbarkley.jsim.prob;

import lombok.Value;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Stream;

import static ca.mbarkley.jsim.util.StreamUtils.product;
import static com.codepoetics.protonpack.StreamUtils.unfold;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;

@Value
public class Event<T> {
    T value;
    double probability;

    public static <T> Stream<Event<T>> productOfIndependent(List<Stream<Event<T>>> streams, BinaryOperator<T> combiner) {
        final Optional<Stream<Event<T>>> product = streams.stream()
                                                          .reduce((lefts, rights) -> productOfIndependent(lefts,
                                                                                                          rights,
                                                                                                          combiner));

        return product.stream().flatMap(Function.identity());
    }

    public static <T, U> Stream<Event<U>> productOfIndependent(Stream<Event<T>> lefts, Stream<Event<T>> rights, BiFunction<T, T, U> combiner) {
        return product(lefts,
                       rights,
                       (l, r) -> new Event<U>(combiner.apply(l.getValue(), r.getValue()), l.getProbability() * r.getProbability()))
                .collect(groupingBy(Event::getValue, reducing(0.0, Event::getProbability, Double::sum)))
                .entrySet()
                .stream()
                .map(e -> new Event<>(e.getKey(), e.getValue()));
    }

    private static <T> Event<T> merge(Event<T> e1, Event<T> e2) {
        return new Event<>(e1.value, e1.getProbability() + e2.getProbability());
    }

    public static Stream<Event<Integer>> singleDieEvents(int diceSides) {
        return unfold(new Event<>(1, 1.0 / ((double) diceSides)), e -> {
            if (e.getValue() < diceSides) {
                return Optional.of(new Event<>(e.getValue() + 1, e.getProbability()));
            } else {
                return Optional.empty();
            }
        });
    }
}
