grammar JSim;

// Lexer rules
WHITESPACE : (' ' | '\t')+ -> skip;

NUMBER : [0-9]+;

D : ('d' | 'D');

PLUS : '+';

MINUS : '-';

// Grammar rules

jsim : expression EOF;

expression : simpleExpression PLUS expression |
             simpleExpression MINUS expression |
             simpleExpression;

simpleExpression : (constant | singleRoll | multiRoll);

constant : NUMBER;

singleRoll : D NUMBER;

multiRoll : NUMBER D NUMBER;
