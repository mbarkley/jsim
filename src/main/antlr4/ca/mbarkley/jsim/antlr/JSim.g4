grammar JSim;

// Lexer rules
WHITESPACE : (' ' | '\t')+ -> skip;

NUMBER : [0-9]+;

D : ('d' | 'D');

H : ('h' | 'H');

L : ('l' | 'L');

PLUS : '+';

MINUS : '-';

LT : '<';

GT : '>';

EQ : '=';

// Grammar rules

jsim : (question | expression) EOF;

question : expression LT expression |
           expression GT expression |
           expression EQ expression;

expression : simpleExpression PLUS expression |
             simpleExpression MINUS expression |
             simpleExpression;

simpleExpression : (constant | singleRoll | multiRoll | highRoll | lowRoll);

constant : NUMBER;

singleRoll : D NUMBER;

multiRoll : NUMBER D NUMBER;

highRoll : NUMBER D NUMBER H NUMBER;

lowRoll : NUMBER D NUMBER L NUMBER;
