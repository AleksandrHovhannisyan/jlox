# jlox

A Java compiler for the Lox programming language. Written as a follow-along of the book [Crafting Interpreters](https://craftinginterpreters.com) by Bob Nystrom.

## Getting Started

Ensure you have the latest version of Java installed for your system. I used Java 17 for this project, specifically openjdk 17.0.9, on WSL 2.

1. Clone the repo.
2. Run `make build` to build the executables.
3. Then, you can run either:
  - `make run` to run the REPL interpreter, or
  - `make run FILE=path/to/test/file.lox` to execute a `.lox` source file.

