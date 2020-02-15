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

expression : arithmeticExpression |
             booleanExpression |
             reference;

booleanExpression : LB booleanExpression RB |
                    booleanExpression AND booleanExpression |
                    booleanExpression OR booleanExpression |
                    booleanExpression EQ booleanExpression |
                    arithmeticComparison |
                    booleanLiteral |
                    reference;

arithmeticComparison : arithmeticExpression LT arithmeticExpression |
                       arithmeticExpression GT arithmeticExpression |
                       arithmeticExpression EQ arithmeticExpression;

arithmeticExpression : LB arithmeticExpression RB |
                       arithmeticExpression DIVIDE arithmeticExpression |
                       arithmeticExpression TIMES arithmeticExpression |
                       arithmeticExpression (PLUS|MINUS) arithmeticExpression |
                       arithmeticLiteral |
                       reference;

reference: IDENTIFIER;

arithmeticLiteral : NUMBER | ROLL | vectorLiteral | SYMBOL;

booleanLiteral : TRUE | FALSE;

vectorLiteral : LCB dimension (COMMA dimension)* RCB;

dimension : SYMBOL COLON dimensionValue;

dimensionValue : IDENTIFIER | NUMBER;
