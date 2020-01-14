parser grammar JSimParser;

options {
    tokenVocab = JSimLexer;
}

// Grammar rules
jsim : statement? (TERMINATOR+ statement)* TERMINATOR* EOF;

statement : expression | definition;

definition : DEFINE IDENTIFIER EQ definitionBody;

definitionBody : expression |
                 diceDeclaration;

diceDeclaration : LSB diceSideDeclaration (COMMA diceSideDeclaration)* RSB;

diceSideDeclaration : NUMBER | TRUE | FALSE | SYMBOL | vectorLiteral;

expression : IDENTIFIER |
             SYMBOL |
             arithmeticExpression |
             booleanExpression;

booleanExpression : LB booleanExpression RB |
                    booleanExpression AND booleanExpression |
                    booleanExpression OR booleanExpression |
                    booleanExpression EQ booleanExpression |
                    symbolTerm EQ symbolTerm |
                    arithmeticComparison |
                    booleanTerm;

arithmeticComparison : arithmeticExpression LT arithmeticExpression |
                       arithmeticExpression GT arithmeticExpression |
                       arithmeticExpression EQ arithmeticExpression;

arithmeticExpression : LB arithmeticExpression RB |
                       arithmeticExpression DIVIDE arithmeticExpression |
                       arithmeticExpression TIMES arithmeticExpression |
                       arithmeticExpression (PLUS|MINUS) arithmeticExpression |
                       arithmeticTerm;

arithmeticTerm : NUMBER | ROLL | IDENTIFIER | vectorLiteral;

booleanTerm : TRUE | FALSE | IDENTIFIER;

symbolTerm : SYMBOL | IDENTIFIER;

vectorLiteral : LCB dimension (COMMA dimension)* RCB;

dimension : SYMBOL COLON dimensionValue;

dimensionValue : IDENTIFIER | NUMBER;
