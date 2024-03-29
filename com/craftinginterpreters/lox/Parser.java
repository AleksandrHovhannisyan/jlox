package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
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
     *
     * <program>                ::= <declaration>* EOF
     * <declaration>            ::= <varDeclaration> | <functionDeclaration> | <statement>
     * <varDeclaration>         ::= "var" IDENTIFIER ( "=" <expression> )? ";"
     * <functionDeclaration>    ::= "fun" <function>
     * <function>               ::= IDENTIFIER "(" <parameters>? ")" <block>
     * <parameters>             ::= IDENTIFIER ( "," IDENTIFIER )*
     *
     * <statement>              ::= <printStatement> | 
     *                              <expressionStatement> | 
     *                              <blockStatement> | 
     *                              <ifStatement> |
     *                              <whileStatement>
     *                              <forStatement>
     *
     * <printStatement>         ::= "print " <expression> ";"
     * <expressionStatement>    ::= <expression> ";"
     * <blockStatement>         ::= "{" <declaration>* "}"
     * <ifStatement>            ::= "if" "(" <expression> ")" <statement> ( "else" <statement> )?
     * <whileStatement>         ::= "while" "(" <expression> ")" <statement>
     * <forStatement>           ::= "for" "(" ( <varDeclaration> | <expressionStatement> | ";" )
     *                                  <expression>? ";" 
     *                                  <expression>? ")" <statement>
     *
     * <expression>             ::= <assignment>
     * <assignment>             ::= IDENTIFIER "=" <assignment> | <equality>
     * <equality>               ::= <comparison> ( ("!="|"==") <comparison> )*
     * <comparison>             ::= <term> ( (">"|">="|"<"|"<=") <term> )*
     * <term>                   ::= <factor> ( ("-"|"+") <factor> )*
     * <factor>                 ::= <unary> ( ("/"|"*") <unary> )*
     * <unary>                  ::= ("!"|"-") <unary> | <call>
     * <call>                   ::= <primary> ( "(" <arguments>? ")" )*
     * <arguments>              ::= <expression> ( "," <expression> )*
     *
     * <primary>                ::= "false" | 
     *                              "true" | 
     *                              "nil" | 
     *                              "(" <expression> ")" | 
     *                              IDENTIFIER
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
            if (consumedTokenMatches(TokenType.VAR)) return varDeclaration();
            if (consumedTokenMatches(TokenType.FUN)) return function();
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
        if (consumedTokenMatches(TokenType.EQUAL)) {
            initializer = expression();
        }
        expectToken(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    /* <functionDeclaration>    ::= "fun" <function>
     * <function>               ::= IDENTIFIER "(" <parameters>? ")" <block>
     * <parameters>             ::= IDENTIFIER ( "," IDENTIFIER )*
     */
    private Stmt function(String functionType) {
        Token name = expectToken(TokenType.IDENTIFIER, "Expect " + functionType + " name.");
        expectToken(TokenType.LEFT_PAREN, "Expect '(' after " + functionType + " name.");
        List<Token> parameters = new ArrayList<>();
        // Non-empty parameter list
        if (!isTokenOfType(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() == 255) {
                    syntaxError(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(expectToken(TokenType.IDENTIFIER, "Expect parameter name."));
            } while (consumedTokenMatches(TokenType.COMMA));
        }
        expectToken(TokenType.RIGHT_PAREN, "Expect ')' after " + functionType + " parameter list.");
        expectToken(TokenType.LEFT_BRACE, "Expect '{' before " + functionType + " body.");
        List<Stmt> body = getBlockStatements();
        return new Stmt.Function(name, parameters, body);
    }

    /** <statement> ::= <printStatement> | <expressionStatement> | <blockStatement> | <whileStatement> */
    private Stmt statement() {
        if (consumedTokenMatches(TokenType.PRINT)) return printStatement();
        if (consumedTokenMatches(TokenType.LEFT_BRACE)) return blockStatement();
        if (consumedTokenMatches(TokenType.IF)) return ifStatement();
        if (consumedTokenMatches(TokenType.WHILE)) return whileStatement();
        if (consumedTokenMatches(TokenType.FOR)) return forStatement();
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

    /** <blockStatement> ::= "{" <declaration>* "}" */
    private Stmt blockStatement() {
        return new Stmt.Block(getBlockStatements());
    }

    /** <ifStatement> ::= "if" "(" <expression> ")" <statement> ( "else" <statement> )? */
    private Stmt ifStatement() {
        expectToken(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        expectToken(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (consumedTokenMatches(TokenType.ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    /** <whileStatement> ::= "while" "(" <expression> ")" <statement> */
    private Stmt whileStatement() {
        expectToken(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        expectToken(TokenType.RIGHT_PAREN, "Expect ')' after while condition.");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    /** <forStatement> ::= "for" "(" ( <varDeclaration> | <expressionStatement> | ";" ) <expression>? ";" <expression> ")" <statement> */
    private Stmt forStatement() {
        expectToken(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");
        Stmt initializer;
        if (consumedTokenMatches(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (consumedTokenMatches(TokenType.VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }
        Expr condition = null;
        if (!consumedTokenMatches(TokenType.SEMICOLON)) {
            condition = expression();
        }
        expectToken(TokenType.SEMICOLON, "Expect ';' after for loop condition.");
        Expr increment = null;
        // Don't consume right paren yet
        if (!isTokenOfType(TokenType.RIGHT_PAREN)) {
            increment = expression();
        }
        expectToken(TokenType.RIGHT_PAREN, "Expect ')' after for loop increment.");
        Stmt body = statement();

        // Desugar for loop to equivalent while loop
        if (increment != null) {
            // Treat body as a block. Increment after body.
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }
        // If no condition, then while(true)
        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);
        // Initializer runs once before body
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }
        // Our interpreter already knows how to handle while statements, so just return the body
        return body;
    }

    /** Helper that parses declarations between opening and closing braces. Used both for block statements and parsing function bodies. */
    private List<Stmt> getBlockStatements() {
        List<Stmt> statements = new ArrayList<Stmt>();
        while (!isTokenOfType(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        expectToken(TokenType.RIGHT_BRACE, "Expect closing brace.");
        return statements;
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
        while (consumedTokenMatches(TokenType.COMMA)) {
            Token operator = getMatchedToken();
            Expr right = equality();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /** <assignment> ::= IDENTIFIER "=" <assignment> | <logic_or> */
    private Expr assignment() {
        // A typical recursive-descent parser has only one token of lookahead. This poses a challenge for assignment because we don't know what we're assigning to until after we parse the left-hand expression and then reach the assignment operator.
        
        // To solve this, we: "parse the left-hand side as if it were an expression..."
        Expr expr = logicalOr();

        if (consumedTokenMatches(TokenType.EQUAL)) {
            Token equalityOperator = getMatchedToken();
            Expr value = assignment();

            // "... and then after the fact produce a syntax tree that turns it into an assignment target."
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assignment(name, value);
            }

            // "If the left-hand side expression isn’t a valid assignment target, we fail with a syntax error."
            syntaxError(equalityOperator, "Invalid assignment target.");
        }

        return expr;
    }

    /** <logic_or> ::= <logic_and> ( "or" <logic_and> )* */
    private Expr logicalOr() {
        Expr expr = logicalAnd();
        while (consumedTokenMatches(TokenType.OR)) {
            Token operator = getMatchedToken();
            Expr right = logicalAnd();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    /** <logic_and> ::= <equality> ( "and" <equality> )* */
    private Expr logicalAnd() {
        Expr expr = equality();
        while (consumedTokenMatches(TokenType.AND)) {
            Token operator = getMatchedToken();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    /** <equality> ::= <comparison> ( ("!="|"==") <comparison> )* */
    private Expr equality() {
        Expr expr = comparison();
        while (consumedTokenMatches(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = getMatchedToken();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /** <comparison> ::= <term> ( (">"|">="|"<"|"<=") <term> )* */
    private Expr comparison() {
        Expr expr = term();
        while (consumedTokenMatches(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = getMatchedToken();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /** <term> ::= <factor> ( ("-"|"+") <factor> )* */
    private Expr term() {
        Expr expr = factor();
        while (consumedTokenMatches(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = getMatchedToken();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
   
    /** <factor> ::= <unary> ( ("/"|"*") <unary> )* */
    private Expr factor() {
        Expr expr = unary();
        while (consumedTokenMatches(TokenType.SLASH, TokenType.STAR)) {
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
        if (consumedTokenMatches(TokenType.BANG, TokenType.MINUS)) {
            Token operator = getMatchedToken();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    /** <call> ::= <primary> ( "(" <arguments>? ")" )* */
    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (consumedTokenMatches(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }

        return expr;
    }

    /** <arguments> ::= <expression> ( "," <expression> )* */
    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!isTokenOfType(TokenType.RIGHT_PAREN)) {
            do {
                // TODO: move 255 to Lox class as a static property
                if (arguments.size() == 255) {
                    syntaxError(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (consumedTokenMatches(TokenType.COMMA));
        }
        Token closingParenthesis = expectToken(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, closingParenthesis, arguments);
    }
   
    /** <primary> ::= "false" | "true" | "nil" | "(" <expression> ")" | IDENTIFIER */
    private Expr primary() {
        if (consumedTokenMatches(TokenType.FALSE)) return new Expr.Literal(false);
        if (consumedTokenMatches(TokenType.TRUE)) return new Expr.Literal(true);
        if (consumedTokenMatches(TokenType.NIL)) return new Expr.Literal(null);
        if (consumedTokenMatches(TokenType.NUMBER, TokenType.STRING)) return new Expr.Literal(getMatchedToken().literal);
        if (consumedTokenMatches(TokenType.IDENTIFIER)) return new Expr.Variable(getMatchedToken());
        if (consumedTokenMatches(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            expectToken(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw syntaxError(peek(), "Expect expression.");
    }

    /** Returns `true` if the current token we are looking at matches one of the given types and `false` otherwise. 
     *  NOTE: If it matches, the parser will advance one position and consume that token. */
    private boolean consumedTokenMatches(TokenType... types) {
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
        throw syntaxError(peek(), message);
    }

    /** Reports a syntax error to the user and returns that error so it can be thrown. */
    private ParseError syntaxError(Token token, String message) {
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
