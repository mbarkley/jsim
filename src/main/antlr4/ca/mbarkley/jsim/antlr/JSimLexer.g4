lexer grammar JSimLexer;

// Lexer rules
WHITESPACE : (' ' | '\t')+ -> skip;

NUMBER : [0-9]+;

TRUE : 'true';

FALSE : 'false';

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

TERMINATOR : '\n' | ';';
