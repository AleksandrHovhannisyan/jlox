package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

class Parser {
    private static class ParseError extends RuntimeException {}

    private List<Token> tokens;
    private int current;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.current = 0;
    }
 
    List<Stmt> parse() {
        try {
            return program();
        } catch (ParseError error) {
            return null;
        }
    }

    /**
     * ========================================================================================
     * Grammar production rules. Rules at the top are lower precedence than ones at the bottom.
     * First introduced in Chapter 6 and then refined in later chapters (e.g., 8 introduces <program>).
     * ========================================================================================
     */ 

    /** <program> ::= <declaration>* EOF */
    private List<Stmt> program() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    /** <declaration> ::= <varDeclaration> | <statement> */
    private Stmt declaration() {
        // From the book: "This declaration() method is the method we call repeatedly when parsing a series of statements in a block or a script, so it’s the right place to synchronize when the parser goes into panic mode. The whole body of this method is wrapped in a try block to catch the exception thrown when the parser begins error recovery. This gets it back to trying to parse the beginning of the next statement or declaration."
        try {
            if (matches(TokenType.VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    /** <varDeclaration> ::= "var" IDENTIFIER ( "=" <expression> )? ";" */
    private Stmt varDeclaration() {
        Token name = expectToken(TokenType.IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        // Assignment
        if (matches(TokenType.EQUAL)) {
            initializer = expression();
        }
        expectToken(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    /** <statement> ::= <printStatement> | <expressionStatement> */
    private Stmt statement() {
        if (matches(TokenType.PRINT)) return printStatement();
        return expressionStatement();
    }

    /** <printStatement> ::= "print " <expression> ";" */
    private Stmt printStatement() {
        Expr expr = expression();
        expectToken(TokenType.SEMICOLON, "Expect semicolon after expression.");
        return new Stmt.Print(expr);
    }

    /** <expressionStatement> ::= <expression> ";" */
    private Stmt expressionStatement() {
        Expr expr = expression();
        expectToken(TokenType.SEMICOLON, "Expect semicolon after expression statement.");
        return new Stmt.Expression(expr);
    }

    /** <expression> ::= <assignment> */
    private Expr expression() {
        return assignment();
    }

    /** Chapter 6 challenge 1: C-style comma operator, lowest precedence. TODO: re-enable after we add parsing for function args
     * <comma> ::= <assignment> ("," <assignment>)*
     */
    private Expr comma() {
        Expr expr = assignment();
        while (matches(TokenType.COMMA)) {
            Token operator = getMatchedToken();
            Expr right = equality();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /** <assignment> ::= IDENTIFIER "=" <assignment> | <equality> */
    private Expr assignment() {
        // A typical recursive-descent parser has only one token of lookahead. This poses a challenge for assignment because we don't know what we're assigning to until after we parse the left-hand expression and then reach the assignment operator.
        
        // To solve this, we: "parse the left-hand side as if it were an expression..."
        Expr expr = equality();

        if (matches(TokenType.EQUAL)) {
            Token equalityOperator = getMatchedToken();
            Expr value = assignment();

            // "... and then after the fact produce a syntax tree that turns it into an assignment target."
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assignment(name, value);
            }

            // "If the left-hand side expression isn’t a valid assignment target, we fail with a syntax error."
            error(equalityOperator, "Invalid assignment target.");
        }

        return expr;
    }

    /** <equality> ::= <comparison> ( ("!="|"==") <comparison> )* */
    private Expr equality() {
        Expr expr = comparison();
        while (matches(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = getMatchedToken();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /** <comparison> ::= <term> ( (">"|">="|"<"|"<=") <term> )* */
    private Expr comparison() {
        Expr expr = term();
        while (matches(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = getMatchedToken();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /** <term> ::= <factor> ( ("-"|"+") <factor> )* */
    private Expr term() {
        Expr expr = factor();
        while (matches(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = getMatchedToken();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
   
    /** <factor> ::= <unary> ( ("/"|"*") <unary> )* */
    private Expr factor() {
        Expr expr = unary();
        while (matches(TokenType.SLASH, TokenType.STAR)) {
            Token operator = getMatchedToken();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /** <unary> ::= ("!"|"-") <unary> 
     *            | <primary>
     */
    private Expr unary() {
        if (matches(TokenType.BANG, TokenType.MINUS)) {
            Token operator = getMatchedToken();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }
   
    /** <primary> ::= "false" | "true" | "nil" | "(" <expression> ")" | IDENTIFIER */
    private Expr primary() {
        if (matches(TokenType.FALSE)) return new Expr.Literal(false);
        if (matches(TokenType.TRUE)) return new Expr.Literal(true);
        if (matches(TokenType.NIL)) return new Expr.Literal(null);
        if (matches(TokenType.NUMBER, TokenType.STRING)) return new Expr.Literal(getMatchedToken().literal);
        if (matches(TokenType.IDENTIFIER)) return new Expr.Variable(getMatchedToken());
        if (matches(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            expectToken(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression.");
    }

    /** Returns `true` if the current token we are looking at matches one of the given types and `false` otherwise. 
     *  NOTE: If it matches, the parser will advance one position. */
    private boolean matches(TokenType... types) {
        for (TokenType type : types) {
            if (isTokenOfType(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /** Returns `true` if the current token we are looking at matches one of the given types and `false` otherwise. */
    private boolean isTokenOfType(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    /** If possible, advances the parser one position and returns the token at that new position. */
    private Token advance() {
        if (!isAtEnd()) current++;
        return getMatchedToken();
    }

    /** Returns `true` if the parser has reached the final token in the source. */
    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }
    
    /** Returns the token at the current position. */
    private Token peek() {
        return tokens.get(current);
    }

    /** Returns the token at the previous position. */
    private Token getMatchedToken() {
        return tokens.get(current - 1);
    }

    /** "Swallows" (consumes) the token of the specified type and returns it to the caller. If the token types do not match, throws an error. */
    private Token expectToken(TokenType type, String message) {
        if (isTokenOfType(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    /** Reports an error to the user and returns that error so it can be thrown. */
    private ParseError error(Token token, String message) {
        Lox.reportError(token, message);
        return new ParseError();
    }

    /** Synchronizes the parser to a valid state after it has encountered an error. 
     * This is done by discarding tokens until we think we have found a statement boundary (usually semicolon).
     */
    private void synchronize() {
        // Move past the token that triggered the error
        advance();

        // Move past all tokens until we reach a semicolon
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
            // Advance past the semicolon to synchronize to the start of the next statement
            advance();
        }
    }
}
