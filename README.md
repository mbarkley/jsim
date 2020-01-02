# JSIM

A simple CLI tool for generating histograms on probabilities of dice roll outcomes.

## Building

Use Maven to build:

```bash
mvn clean package
```

You will find a standalone jar in the target directory.

## Usage

For convenience, create an alias for running the jar (modifying the path to the jar as necessary):

```bash
alias jsim="java -jar $HOME/jsim/target/jsim*.jar"
```

Sample input:
```bash
$ jsim 2d6
```

Sample output:
```bash
--------------------------------------------------------- 2d6  ---------------------------------------------------------
2  |******************                                                                                                  2.78%
3  |*************************************                                                                               5.56%
4  |*******************************************************                                                             8.33%
5  |**************************************************************************                                          11.11%
6  |********************************************************************************************                        13.89%
7  |***************************************************************************************************************     16.67%
8  |********************************************************************************************                        13.89%
9  |**************************************************************************                                          11.11%
10 |*******************************************************                                                             8.33%
11 |*************************************                                                                               5.56%
12 |******************                                                                                                  2.78%
```

### Types of expressions

The following base terms are supported:

1. `d20` (Single dice)
2. `2d6` (Multiple dice of same kind)
3. `2d6H1` or `2d6L1` (Highest or lowest *n* dice of kind)

Additionally, any terms can be combined with brackets, addition, subtraction, multiplication, and integer division:
1. `d8 + 2d6` or `d8 - 2d6 + 1`(Sums of dice and constants)
2. `d6 * 2` or `d6 * d4` (Multiplication by constant or other dice)
3. `d6 / 2` or `d6 / d4` (Division by constant or other dice)
4. `(2d6 + 1) * 2` (Brackets)

You can also also use `<`, `>`, and `=` predicates that will print histograms for the true or false probabilities:
1. `d2d6 + 1 > 6`
2. `2d6 + 1 < 2d6`
3. `d8 = d6`
