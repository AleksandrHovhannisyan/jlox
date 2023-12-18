package com.craftinginterpreters.lox;

class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int lineNumber;

    Token(TokenType type, String lexeme, Object literal, int lineNumber) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.lineNumber = lineNumber;
    }

    public String toString() {
        return type + " " + lexeme + " on line " + lineNumber + ": " + literal;
    }
}
