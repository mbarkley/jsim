package ca.mbarkley.jsim.prob;

import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ca.mbarkley.jsim.util.StreamUtils.product;
import static java.util.stream.Collectors.reducing;

@Value
public class Event<T> {
    T value;
    BigDecimal probability;

    public static <T> Stream<Event<T>> productOfIndependent(List<Stream<Event<T>>> streams, BinaryOperator<T> combiner) {
        final Optional<Stream<Event<T>>> product = streams.stream()
                                                          .reduce((lefts, rights) -> productOfIndependent(lefts,
                                                                                                          rights,
                                                                                                          combiner::apply));

        return product.stream().flatMap(Function.identity());
    }

    public static <T, U> Stream<Event<U>> productOfIndependent(Stream<Event<T>> lefts, Stream<Event<T>> rights, BiFunction<T, T, U> combiner) {
        return product(lefts,
                       rights,
                       (l, r) -> new Event<U>(combiner.apply(l.getValue(), r.getValue()), l.getProbability().multiply(r.getProbability())))
                .collect(Collectors.groupingBy(Event::getValue, reducing(Event::merge)))
                .entrySet()
                .stream()
                .flatMap(o -> o.getValue().stream());
    }

    private static <T> Event<T> merge(Event<T> e1, Event<T> e2) {
        return new Event<>(e1.value, e1.getProbability().add(e2.getProbability()));
    }
}
