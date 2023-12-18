package com.craftinginterpreters.lox;

import java.util.List;

class Parser {
    private List<Token> tokens;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }
}
