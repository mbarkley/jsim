package ca.mbarkley.jsim.util;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class StreamUtils {
    public static <T, U, V> Stream<V> product(Stream<T> left, Stream<U> right, BiFunction<T, U, V> combiner) {
        final List<U> savedRights = right.collect(toList());
        return left.flatMap(l -> savedRights.parallelStream()
                                            .map(r -> combiner.apply(l, r)))
                   .parallel();
    }
}
