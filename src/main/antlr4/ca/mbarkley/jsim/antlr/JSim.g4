grammar JSim;

// Lexer rules
WHITESPACE : (' ' | '\t')+ -> skip;

NUMBER : [0-9]+;

D : ('d' | 'D');

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

simpleExpression : (constant | singleRoll | multiRoll);

constant : NUMBER;

singleRoll : D NUMBER;

multiRoll : NUMBER D NUMBER;
