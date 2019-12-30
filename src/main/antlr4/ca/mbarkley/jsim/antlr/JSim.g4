grammar JSim;

// Lexer rules
WHITESPACE : (' ' | '\t')+ -> skip;

NUMBER : [0-9]+;

D : ('d' | 'D');

PLUS : '+';

MINUS : '-';

// Grammar rules

jsim : expression EOF;

expression : simpleExpression (operator NUMBER)?;

operator : (PLUS | MINUS);

simpleExpression : (NUMBER | singleRoll | multiRoll);

singleRoll : D NUMBER;

multiRoll : NUMBER D NUMBER;
