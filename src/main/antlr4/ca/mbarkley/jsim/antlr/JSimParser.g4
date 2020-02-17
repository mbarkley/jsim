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

diceSideDeclaration : expression;

expression : arithmeticExpression |
             booleanExpression;

booleanExpression : LB booleanExpression RB |
                    booleanExpression AND booleanExpression |
                    booleanExpression OR booleanExpression |
                    arithmeticExpression EQ arithmeticExpression |
                    booleanExpression EQ booleanExpression |
                    arithmeticComparison |
                    booleanLiteral |
                    reference |
                    letExpression;

arithmeticComparison : arithmeticExpression LT arithmeticExpression |
                       arithmeticExpression GT arithmeticExpression;

arithmeticExpression : LB arithmeticExpression RB |
                       arithmeticExpression DIVIDE arithmeticExpression |
                       arithmeticExpression TIMES arithmeticExpression |
                       arithmeticExpression (PLUS|MINUS) arithmeticExpression |
                       multiplicativeTerm |
                       arithmeticLiteral |
                       reference |
                       vectorComponentRestriction |
                       letExpression;

letExpression : LET IDENTIFIER EQ expression IN expression;

vectorComponentRestriction: (reference | vectorLiteral) LCB SYMBOL RCB;

multiplicativeTerm: NUMBER (reference | SYMBOL);

reference: IDENTIFIER;

arithmeticLiteral : NUMBER | ROLL | vectorLiteral | SYMBOL;

booleanLiteral : TRUE | FALSE;

vectorLiteral : LCB dimension (COMMA dimension)* RCB;

dimension : SYMBOL COLON dimensionValue;

dimensionValue : IDENTIFIER | NUMBER;
