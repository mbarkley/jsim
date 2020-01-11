parser grammar JSimParser;

options {
    tokenVocab = JSimLexer;
}

// Grammar rules
jsim : statement? (TERMINATOR+ statement)* TERMINATOR* EOF;

statement : arithmeticExpression |
            booleanExpression;

booleanExpression : LB booleanExpression RB |
                    booleanExpression AND booleanExpression |
                    booleanExpression OR booleanExpression |
                    booleanTerm;

booleanTerm : arithmeticExpression LT arithmeticExpression |
              arithmeticExpression GT arithmeticExpression |
              arithmeticExpression EQ arithmeticExpression |
              booleanLiteral;

arithmeticExpression : LB arithmeticExpression RB |
                       arithmeticExpression DIVIDE arithmeticExpression |
                       arithmeticExpression TIMES arithmeticExpression |
                       arithmeticExpression (PLUS|MINUS) arithmeticExpression |
                       arithmeticTerm;

arithmeticTerm : numberLiteral |
                 singleRoll |
                 multiRoll |
                 highRoll |
                 lowRoll;

booleanLiteral : TRUE | FALSE;

numberLiteral : NUMBER;

singleRoll : D NUMBER;

multiRoll : NUMBER D NUMBER;

highRoll : NUMBER D NUMBER H NUMBER;

lowRoll : NUMBER D NUMBER L NUMBER;
