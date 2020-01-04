grammar JSim;

// Lexer rules
WHITESPACE : (' ' | '\t')+ -> skip;

NUMBER : [0-9]+;

// Dice pool symbols
D : ('d' | 'D');

H : ('h' | 'H');

L : ('l' | 'L');

// Arithmetic symbols
TIMES : '*';

DIVIDE : '/';

PLUS : '+';

MINUS : '-';

LB : '(';

RB: ')';

// Relation symbols
LT : '<';

GT : '>';

EQ : '=';

// Boolean operators
AND : 'and';

OR : 'or';

// Grammar rules
jsim : (question | expression) EOF;

question : predicate AND question |
           predicate OR question |
           predicate;

predicate : expression LT expression |
            expression GT expression |
            expression EQ expression;

expression : term TIMES expression |
             term DIVIDE expression |
             term PLUS expression |
             term MINUS expression |
             term;

term : LB expression RB |
       atom;

atom : constant | singleRoll | multiRoll | highRoll | lowRoll;

constant : NUMBER;

singleRoll : D NUMBER;

multiRoll : NUMBER D NUMBER;

highRoll : NUMBER D NUMBER H NUMBER;

lowRoll : NUMBER D NUMBER L NUMBER;
