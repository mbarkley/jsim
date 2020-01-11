grammar JSim;

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

// Grammar rules
jsim : statement? (TERMINATOR+ statement)* TERMINATOR* EOF;

statement : arithmeticExpression |
            booleanExpression;

booleanExpression : booleanTerm AND booleanExpression |
                    booleanTerm OR booleanExpression |
                    booleanTerm;

booleanTerm : arithmeticExpression LT arithmeticExpression |
              arithmeticExpression GT arithmeticExpression |
              arithmeticExpression EQ arithmeticExpression |
              booleanLiteral;

arithmeticExpression : arithmeticTerm TIMES arithmeticExpression |
                       arithmeticTerm DIVIDE arithmeticExpression |
                       arithmeticTerm PLUS arithmeticExpression |
                       arithmeticTerm MINUS arithmeticExpression |
                       arithmeticTerm;

arithmeticTerm : LB arithmeticExpression RB |
                 numberLiteral |
                 singleRoll |
                 multiRoll |
                 highRoll |
                 lowRoll;

booleanLiteral : TRUE | FALSE;

numberLiteral : NUMBER;

singleRoll : D NUMBER;

multiRoll : NUMBER D NUMBER;

highRoll : NUMBER D NUMBER H NUMBER;

lowRoll : NUMBER D NUMBER L NUMBER;
