package ca.mbarkley.jsim.model;

import ca.mbarkley.jsim.eval.RuntimeContext;
import ca.mbarkley.jsim.model.ExpressionConverter.ValueConverter;
import ca.mbarkley.jsim.prob.Event;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static ca.mbarkley.jsim.model.BinaryOperators.lookupBinaryOp;
import static ca.mbarkley.jsim.prob.Event.productOfIndependent;
import static ca.mbarkley.jsim.util.FormatUtils.formatAsPercentage;
import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public abstract class Expression<T extends Comparable<T>> {
    protected Expression() {}

    public abstract boolean isConstant();
    public abstract Stream<Event<T>> events(RuntimeContext ctx);

    public Map<T, Event<T>> calculateResults() {
        final RuntimeContext ctx = new RuntimeContext(Map.of());
        return events(ctx).collect(toMap(Event::getValue, identity(), (e1, e2) -> new Event<>(e1.getValue(), e1.getProbability() + e2.getProbability())));
    }

    public abstract Type<T> getType();

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MultiplicativeExpression extends Expression<Vector> {
        Integer number;
        Expression<Vector> subExpression;

        @Override
        public Stream<Event<Vector>> events(RuntimeContext ctx) {
            final List<Stream<Event<Vector>>> singleEventStreams = Stream.generate(() -> subExpression.events(ctx))
                                                                         .limit(number)
                                                                         .collect(toList());
            final BinaryOperator<Vector, Vector> op = (BinaryOperator<Vector, Vector>) lookupBinaryOp(subExpression.getType(), subExpression.getType(), "+").get();

            return productOfIndependent(singleEventStreams, op::evaluate);
        }

        @Override
        public boolean isConstant() {
            return subExpression.isConstant();
        }

        @Override
        public String toString() {
            return format("(%s)", subExpression);
        }

        @Override
        public Type<Vector> getType() {
            return subExpression.getType();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Bracketed<T extends Comparable<T>> extends Expression<T> {
        Expression<T> subExpression;

        @Override
        public Stream<Event<T>> events(RuntimeContext ctx) {
            return subExpression.events(ctx);
        }

        @Override
        public boolean isConstant() {
            return subExpression.isConstant();
        }

        @Override
        public String toString() {
            return format("(%s)", subExpression);
        }

        @Override
        public Type<T> getType() {
            return subExpression.getType();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Constant<T extends Comparable<T>> extends Expression<T> {
        Type<T> type;
        T value;

        public Constant(Type<T> type, T value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public Stream<Event<T>> events(RuntimeContext ctx) {
            return Stream.of(new Event<>(value, 1.0));
        }

        @Override
        public boolean isConstant() {
            return true;
        }

        @Override
        public Type<T> getType() {
            return type;
        }

        @Override
        public String toString() {
            return "" + value;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class BoundConstant<T extends Comparable<T>> extends Expression<T> {
        String identifier;
        Type<T> type;

        @Override
        public boolean isConstant() {
            return true;
        }

        @Override
        public Stream<Event<T>> events(RuntimeContext ctx) {
            final Constant<?> foundValue = Optional.ofNullable(ctx.getDefinitions()
                                                                  .get(identifier))
                                                   .orElseThrow(() -> new IllegalStateException(format("Expected runtime context to have binding for [%s] but context was [%s]", identifier, ctx)));

            final T value = type.strictCast(foundValue
                                               .getValue());

            return Stream.of(new Event<>(value, 1.0));
        }

        @Override
        public Type<T> getType() {
            return type;
        }
    }


    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class BindExpression<B extends Comparable<B>, T extends Comparable<T>> extends Expression<T> {
        String boundIdentifier;
        Expression<B> bindExpression;
        Expression<T> valueExpression;

        @Override
        public Stream<Event<T>> events(RuntimeContext ctx) {
            return bindExpression.events(ctx)
                                 // TODO maybe group by same values here?
                                 .flatMap(event -> {
                                     final RuntimeContext subCtx = ctx.with(boundIdentifier, new Constant<>(bindExpression.getType(), event.getValue()));
                                     return valueExpression.events(subCtx)
                                                           .map(subEvent -> new Event<>(subEvent.getValue(), subEvent.getProbability() * event.getProbability()));
                                 });
        }

        @Override
        public boolean isConstant() {
            /*
             * We mostly only care about constants when declaring dice sides, so let's not worry
             * too much about this and return false.
             */
            return false;
        }

        @Override
        public Type<T> getType() {
            return valueExpression.getType();
        }

        @Override
        public String toString() {
            return format("let %s <- %s in %s", boundIdentifier, bindExpression, valueExpression);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MappedExpression<S extends Comparable<S>, T extends Comparable<T>> extends Expression<T> {
        Expression<S> expression;
        ValueConverter<S, T> mapper;

        @Override
        public Stream<Event<T>> events(RuntimeContext ctx) {
            return expression.events(ctx)
                             .map(e -> new Event<>(mapper.convert(e.getValue()), e.getProbability()));
        }

        @Override
        public boolean isConstant() {
            return expression.isConstant();
        }

        @Override
        public Type<T> getType() {
            return mapper.getTargetType();
        }

        @Override
        public String toString() {
            // FIXME there must be something better to do here?
            return expression.toString();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class EventList<T extends Comparable<T>> extends Expression<T> {
        Type<T> type;
        List<Event<T>> values;

        @Override
        public Stream<Event<T>> events(RuntimeContext ctx) {
            return values.stream();
        }

        @Override
        public boolean isConstant() {
            return values.size() == 1 ||
                    values.size() > 1 && values.stream()
                                               .allMatch(v -> values.get(0).getValue().equals(v.getValue()));
        }

        @Override
        public Type<T> getType() {
            return type;
        }

        @Override
        public String toString() {
            final String[] valueString = values.stream()
                                               .map(event -> format("%s (%s)", event.getValue(), formatAsPercentage(event.getProbability())))
                                               .toArray(String[]::new);
            return format("[%s]", String.join(", ", valueString));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class BinaryOpExpression<I extends Comparable<I>, T extends Comparable<T>> extends Expression<T> {
        Expression<I> left;
        BinaryOperator<I, T> operator;
        Expression<I> right;

        @Override
        public Stream<Event<T>> events(RuntimeContext ctx) {
            return productOfIndependent(left.events(ctx), right.events(ctx), operator::evaluate);
        }

        @Override
        public boolean isConstant() {
            return left.isConstant() && right.isConstant();
        }

        @Override
        public Type<T> getType() {
            return operator.getOutputType(left.getType(), right.getType());
        }

        @Override
        public String toString() {
            return format("%s %s %s", left, operator.getSymbol(), right);
        }
    }
}
