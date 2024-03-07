# jlox

A Java compiler for the Lox programming language. Written as a follow-along of the book [Crafting Interpreters](https://craftinginterpreters.com) by Bob Nystrom.

## Lox Grammar

The grammar production rules for Lox are listed below. Rules at the top have a lower precedence than ones at the bottom.

```
<program>                ::= <declaration>* EOF
<declaration>            ::= <varDeclaration> | <statement>
<varDeclaration>         ::= "var" IDENTIFIER ( "=" <expression> )? ";"
<statement>              ::= <printStatement> | <expressionStatement> | <blockStatement> | <ifStatement>
<printStatement>         ::= "print " <expression> ";"
<expressionStatement>    ::= <expression> ";"
<blockStatement>         ::= "{" <declaration>* "}"
<ifStatement> ::= "if" "(" <expression> ")" <statement> ( "else" <statement> )?
<expression>             ::= <assignment>
<assignment>             ::= IDENTIFIER "=" <assignment> | <equality>
<equality>               ::= <comparison> ( ("!="|"==") <comparison> )*
<comparison>             ::= <term> ( (">"|">="|"<"|"<=") <term> )*
<term>                   ::= <factor> ( ("-"|"+") <factor> )*
<factor>                 ::= <unary> ( ("/"|"*") <unary> )*
<unary>                  ::= ("!"|"-") <unary> | <primary>
<primary>                ::= "false" | "true" | "nil" | "(" <expression> ")" | IDENTIFIER
```

## Getting Started

Ensure you have the latest version of Java installed for your system. I used Java 17 for this project, specifically openjdk 17.0.9, on WSL 2.

1. Clone the repo.
2. Run `make build` to build the executables.
3. Then, you can run either:
  - `make run` to run the REPL interpreter, or
  - `make run FILE=path/to/test/file.lox` to execute a `.lox` source file.

