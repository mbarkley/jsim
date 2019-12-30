package ca.mbarkley.jsim;

import ca.mbarkley.jsim.antlr.JSimBaseVisitor;
import ca.mbarkley.jsim.antlr.JSimLexer;
import ca.mbarkley.jsim.antlr.JSimParser;
import ca.mbarkley.jsim.prob.RandomDicePoolVariable;
import ca.mbarkley.jsim.prob.RandomDieVariable;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.parseInt;

public class Parser {
    public RandomDicePoolVariable parse(String expression) throws RecognitionException {
        final ANTLRInputStream is = new ANTLRInputStream(expression);
        final JSimLexer lexer = new JSimLexer(is);
        final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        final JSimParser parser = new JSimParser(tokenStream);
        final JSimParser.JsimContext ctx = parser.jsim();

        final Visitor visitor = new Visitor();
        return visitor.visit(ctx);
    }

    private class Visitor extends JSimBaseVisitor<RandomDicePoolVariable> {
        @Override
        public RandomDicePoolVariable visitSingleRoll(JSimParser.SingleRollContext ctx) {
            final Token rawNumber = ctx.NUMBER().getSymbol();
            final RandomDieVariable die = new RandomDieVariable(parseInt(rawNumber.getText()));
            return new RandomDicePoolVariable(List.of(die), 0);
        }

        @Override
        public RandomDicePoolVariable visitMultiRoll(JSimParser.MultiRollContext ctx) {
            final int diceNumber = parseInt(ctx.NUMBER(0).getSymbol().getText());
            final int diceSides = parseInt(ctx.NUMBER(1).getSymbol().getText());

            final List<RandomDieVariable> dice = Stream.generate(() -> new RandomDieVariable(diceSides))
                                                       .limit(diceNumber)
                                                       .collect(Collectors.toList());

            return new RandomDicePoolVariable(dice, 0);
        }

        @Override
        public RandomDicePoolVariable visitExpression(JSimParser.ExpressionContext ctx) {
            final RandomDicePoolVariable firstDicePool = visitSimpleExpression(ctx.simpleExpression());
            if (ctx.NUMBER() != null) {
                // FIXME use visitor for typesafety
                final int sign = ctx.operator().PLUS() != null ? 1 : -1;
                final int otherMod = parseInt(ctx.NUMBER().getText()) * sign;

                return new RandomDicePoolVariable(firstDicePool.getDice(), firstDicePool.getModifier() + otherMod);
            } else {
                return firstDicePool;
            }
        }

        @Override
        protected RandomDicePoolVariable defaultResult() {
            return new RandomDicePoolVariable(List.of(), 0);
        }

        @Override
        protected RandomDicePoolVariable aggregateResult(RandomDicePoolVariable aggregate, RandomDicePoolVariable nextResult) {
            var dice = new ArrayList<RandomDieVariable>(aggregate.getDice().size() + nextResult.getDice().size());
            dice.addAll(aggregate.getDice());
            dice.addAll(nextResult.getDice());

            return new RandomDicePoolVariable(dice, aggregate.getModifier() + nextResult.getModifier());
        }
    }
}
