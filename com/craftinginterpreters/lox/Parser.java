package com.craftinginterpreters.lox;

import java.util.List;

class Parser {
    private static class ParseError extends RuntimeException {}

    private List<Token> tokens;
    private int current;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.current = 0;
    }
 
    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    /**
     * ========================================================================================
     * Grammar production rules. Rules at the top are lower precedence than ones at the bottom.
     * ========================================================================================
     */ 

    /** <expression> ::= <equality> */
    private Expr expression() {
        return equality();
    }

    /** <equality> ::= <comparison> ("!="|"==") <comparison>)* */
    private Expr equality() {
        Expr expr = comparison();

        while (matches(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = getMatchedToken();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /** <comparison> ::= <term> (">"|">="|"<"|"<=" <term>)* */
    private Expr comparison() {
        Expr expr = term();

        while (matches(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = getMatchedToken();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /** <term> ::= <factor> ("-"|"+" <factor>)* */
    private Expr term() {
        Expr expr = factor();

        while (matches(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = getMatchedToken();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
   
    /** <factor> ::= <unary> ("/"|"*" <unary>)* */
    private Expr factor() {
        Expr expr = unary();

        while (matches(TokenType.SLASH, TokenType.STAR)) {
            Token operator = getMatchedToken();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /** <unary> ::= ("!"|"-" <unary>) | <primary> */
    private Expr unary() {
        if (matches(TokenType.BANG, TokenType.MINUS)) {
            Token operator = getMatchedToken();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }
   
    /** <primary> ::= "false"|"true"|"nil"| "(" <expr> ")" */
    private Expr primary() {
        if (matches(TokenType.FALSE)) return new Expr.Literal(false);
        if (matches(TokenType.TRUE)) return new Expr.Literal(true);
        if (matches(TokenType.NIL)) return new Expr.Literal(null);
        
        if (matches(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(getMatchedToken().literal);
        }

        if (matches(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consumeToken(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private boolean matches(TokenType... types) {
        for (TokenType type : types) {
            if (isTokenOfType(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean isTokenOfType(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return getMatchedToken();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }
    
    private Token peek() {
        return tokens.get(current);
    }

    private Token getMatchedToken() {
        return tokens.get(current - 1);
    }

    private Token consumeToken(TokenType type, String message) {
        if (isTokenOfType(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.reportError(token, message);
        return new ParseError();
    }

    /** Synchronizes the parser to a valid state after it has encountered an error. Discards tokens until it thinks it has found a statement boundary. */
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (getMatchedToken().type == TokenType.SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}
