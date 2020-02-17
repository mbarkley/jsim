parser grammar JSimParser;

options {
    tokenVocab = JSimLexer;
}

// Grammar rules
jsim
    : statement? (TERMINATOR+ statement)* TERMINATOR* EOF
    ;

statement
    : expression
    | definition
    ;

definition
    : DEFINE IDENTIFIER EQ definitionBody
    ;

definitionBody
    : expression
    | diceDeclaration
    ;

diceDeclaration
    : LSB diceSideDeclaration (COMMA diceSideDeclaration)* RSB
    ;

diceSideDeclaration
    : expression
    ;

expression
    : LB expression RB
    | expression LCB SYMBOL RCB
    | expression DIVIDE expression
    | expression TIMES expression
    | expression (PLUS|MINUS) expression
    | expression (LT|GT|EQ) expression
    | expression AND expression
    | expression OR expression
    | letExpression
    | multiplicativeTerm
    | reference
    | literal
    ;

literal
    : booleanLiteral
    | vectorLiteral
    | NUMBER
    | ROLL
    | SYMBOL
    ;

letExpression
    : LET IDENTIFIER LARROW expression IN expression
    ;

multiplicativeTerm
    : NUMBER (reference | SYMBOL)
    ;

reference
    : IDENTIFIER
    ;

booleanLiteral
    : TRUE
    | FALSE
    ;

vectorLiteral
    : LCB (dimension (COMMA dimension)*)? RCB
    ;

dimension
    : SYMBOL COLON dimensionValue
    ;

dimensionValue
    : IDENTIFIER
    | NUMBER
    ;
