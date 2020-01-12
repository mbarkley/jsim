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

diceSideDeclaration : NUMBER | TRUE | FALSE;

expression : IDENTIFIER |
             arithmeticExpression |
             booleanExpression;

booleanExpression : LB booleanExpression RB |
                    booleanExpression AND booleanExpression |
                    booleanExpression OR booleanExpression |
                    comparison;

comparison : arithmeticExpression LT arithmeticExpression |
             arithmeticExpression GT arithmeticExpression |
             arithmeticExpression EQ arithmeticExpression |
             booleanTerm;

arithmeticExpression : LB arithmeticExpression RB |
                       arithmeticExpression DIVIDE arithmeticExpression |
                       arithmeticExpression TIMES arithmeticExpression |
                       arithmeticExpression (PLUS|MINUS) arithmeticExpression |
                       arithmeticTerm;

arithmeticTerm : NUMBER | ROLL | IDENTIFIER;

booleanTerm : TRUE | FALSE | IDENTIFIER;
