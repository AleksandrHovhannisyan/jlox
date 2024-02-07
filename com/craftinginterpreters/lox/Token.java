package com.craftinginterpreters.lox;

/** Represents a lexeme: one or more characters comprising a unit of "meaning" in a grammar. For example, a lexeme may be a single-character operator like `=` or `+`, or it may be a multi-character operator like `==`, `!=`, etc., or it may be a keyword or identifier. */
class Token {
    final TokenType type;
    /** The raw plaintext representation of this token, as it appears in the source code. */
    final String lexeme;
    /** The actual underlying value represented by this token. */
    final Object literal;
    /** The line number on which the token can be found in the source. */
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
