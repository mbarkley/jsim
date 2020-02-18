lexer grammar JSimLexer;

WHITESPACE : (' ' | '\t')+ -> skip;

// Keywords
DEFINE : 'define';

LET : 'let';

LARROW : '<-';

IN : 'in';

NUMBER : [0-9]+;

TRUE : 'true';

FALSE : 'false';

// Arithmetic symbols
TIMES : '*';

DIVIDE : '/';

MOD : '%';

PLUS : '+';

MINUS : '-';

LB : '(';

RB : ')';

LSB : '[';

RSB : ']';

LCB : '{';

RCB : '}';

COLON : ':';

COMMA : ',';

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

IDENTIFIER : [a-zA-Z_][a-zA-Z0-9_]*;

SYMBOL : [':]IDENTIFIER;
