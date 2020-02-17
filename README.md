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

### Command Line Arguments

Use the `-c` argument to pass an expression via command-line argument:
```bash
$ jsim -c 2d6
```

Alternatively, pipe an expression to standard input:
```bash
$ echo 2d6 | jsim
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

### Interactive Mode

Running the jar from an interactive shell without arguments or piped input will result in a repl.

### Types of expressions

#### Built-in Dice Rolls
The following base terms are supported:

1. `d20` (Single dice)
2. `2d6` (Multiple dice of same kind)
3. `2d6H1` or `2d6L1` (Highest or lowest *n* dice of kind)

#### Arithmetic Expressions
Additionally, any terms can be combined with brackets, addition, subtraction, multiplication, and integer division:
1. `d8 + 2d6` or `d8 - 2d6 + 1`(Sums of dice and constants)
2. `d6 * 2` or `d6 * d4` (Multiplication by constant or other dice)
3. `d6 / 2` or `d6 / d4` (Division by constant or other dice)
4. `(2d6 + 1) * 2` (Brackets)

#### Comparisons
You can also also use `<`, `>`, and `=` predicates that will print histograms for the true or false probabilities:
1. `d2d6 + 1 > 6`
2. `2d6 + 1 < 2d6`
3. `d8 = d6`

#### Custom Dice
Define your own dice with arbitrary sides:
```
define die = [1, 3, 5, 9]
die + die
```

This outputs:
```
------------------------------------------------------ die + die  ------------------------------------------------------
2  |*************************************                                                                               6.25%
4  |**************************************************************************                                          12.50%
6  |***************************************************************************************************************     18.75%
8  |**************************************************************************                                          12.50%
10 |***************************************************************************************************************     18.75%
12 |**************************************************************************                                          12.50%
14 |**************************************************************************                                          12.50%
18 |*************************************                                                                               6.25%
```

#### Symbols and Vectors

##### Symbols
Symbols are alphanumeric strings that start with a single `'` or `:` character.

Examples:
* `'foo`
* `:bar`

##### Symbols in Custom Dice
Use symbols in custom dice definitions to model coins or dice that have counted icons.

Example:
```
define coin = ['heads, 'tails]
coin + coin + coin
```

This outputs:
```
-------------------------------------------------- coin + coin + coin --------------------------------------------------
{'heads=0, 'tails=3} |*******************************                                                                   12.50%
{'heads=1, 'tails=2} |*********************************************************************************************     37.50%
{'heads=2, 'tails=1} |*********************************************************************************************     37.50%
{'heads=3, 'tails=0} |*******************************  
```

##### Vector Results
Results like the three coin tosses above are modelled as vectors with named components. You can do comparisons
against vectors in a couple different ways to answer questions about custom dice.

For example, if I want to find the odds of getting exactly one `'heads` in three coin flips I can use any of the following:

* Explicit Vector
    ```
    coin + coin + coin = {'heads: 1, 'tails: 2}
    ```
* Sum of Components
    ```
    coin + coin + coin = 'heads + 2'tails
    ```
* Component Value Access
    ```
    (coin + coin + coin){'heads} = 1
    ```

#### Let Statements

Use let statements to construct complex boolean expressions around a single roll.

For example, here is an expression for the odds that a single roll of 2d6 is less than 4 or greater than 9:

```
let d <- 2d6 in d < 4 or d > 9
```