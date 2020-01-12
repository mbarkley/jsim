lexer grammar JSimLexer;

// Lexer rules
WHITESPACE : (' ' | '\t')+ -> skip;

NUMBER : [0-9]+;

TRUE : 'true';

FALSE : 'false';

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

// Dice pool symbols
fragment D : ('d' | 'D');

fragment H : ('h' | 'H');

fragment L : ('l' | 'L');

ROLL : NUMBER? D NUMBER ((H|L) NUMBER)?;
