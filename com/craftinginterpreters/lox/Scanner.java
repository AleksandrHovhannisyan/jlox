package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

// Lexer. Given a source, transforms it into a list of tokens (lexemes).
class Scanner {
    // Reserved keywords for the language
    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("and",    TokenType.AND);
        keywords.put("class",  TokenType.CLASS);
        keywords.put("else",   TokenType.ELSE);
        keywords.put("false",  TokenType.FALSE);
        keywords.put("for",    TokenType.FOR);
        keywords.put("fun",    TokenType.FUN);
        keywords.put("if",     TokenType.IF);
        keywords.put("nil",    TokenType.NIL);
        keywords.put("or",     TokenType.OR);
        keywords.put("print",  TokenType.PRINT);
        keywords.put("return", TokenType.RETURN);
        keywords.put("super",  TokenType.SUPER);
        keywords.put("this",   TokenType.THIS);
        keywords.put("true",   TokenType.TRUE);
        keywords.put("var",    TokenType.VAR);
        keywords.put("while",  TokenType.WHILE);
    }

    // Source code + parsed tokens
    private final String source;
    private final List<Token> tokens;

    // Positional fields
    private int tokenStartIndex = 0;
    private int currentIndex = 0;
    private int lineNumber = 1;

    Scanner(String source) {
        this.source = source;
        this.tokens = new ArrayList<Token>();
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            tokenStartIndex = currentIndex;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, lineNumber));
        return tokens;
    }

    /** Returns true if the scanner has fully parsed the source. */
    private boolean isAtEnd() {
        return currentIndex >= source.length();
    }

    /** Consumes the token between the start index and the end of the token. */
    private void scanToken() {
       char character = consumeCharacter();
       switch (character) {
           // Single-character tokens
           case '(': addToken(TokenType.LEFT_PAREN); break;
           case ')': addToken(TokenType.RIGHT_PAREN); break;
           case '[': addToken(TokenType.LEFT_BRACE); break;
           case ']': addToken(TokenType.RIGHT_BRACE); break;
           case ',': addToken(TokenType.COMMA); break;
           case '.': addToken(TokenType.DOT); break;
           case '-': addToken(TokenType.MINUS); break;
           case '+': addToken(TokenType.PLUS); break;
           case ';': addToken(TokenType.SEMICOLON); break;
           case '*': addToken(TokenType.STAR); break;
           
           // One- or two-character tokens
           case '!': addToken(isNext('=') ? TokenType.BANG_EQUAL : TokenType.BANG); break;
           case '=': addToken(isNext('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL); break;
           case '>': addToken(isNext('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER); break;
           case '<': addToken(isNext('=') ? TokenType.LESS_EQUAL : TokenType.LESS); break;
           case '/':
                // Consume comment
                if (isNext('/')) {
                    while (peek() != '\n' && !isAtEnd()) consumeCharacter();
                } else {
                    // Division
                    addToken(TokenType.SLASH);
                }
                break;

            // Whitespace
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                lineNumber++;
                break;

            // Strings
            case '"': string(); break;

            // Other
            default:
                if (isDigit(character)) {
                    number();
                } else if (isAlpha(character)) {
                    // First use maximal much/greed and assume that a string of alpha characters is an identifier. We'll check if it's a reserved keyword later.
                    identifier();
                } else {
                    Lox.reportError(lineNumber, "Unexpected character.");
                }
       }
    }

    /** Returns the character at the specified index in the source without advancing (lookahead), or the null terminator character if index is at or past the end of the source. */
    private char getCharAt(int index) {
        if (index >= source.length()) return '\0';
        return source.charAt(index);
    }

    private char peek() {
        return getCharAt(currentIndex);
    }

    private char peekNext() {
        return getCharAt(currentIndex + 1);
    }

    /** Peeks at the next character in the source and returns `true` if it matches the specified character. */
    private boolean isNext(char expectedCharacter) {
        if (isAtEnd()) return false;
        char nextCharacter = source.charAt(currentIndex + 1);
        if (nextCharacter != expectedCharacter) return false;
        // If it did match, we need to consume that character
        consumeCharacter();
        return true;
    }

    /** Consumes the next character in the source and returns it. */
    private char consumeCharacter() {
        return source.charAt(currentIndex++);
    }

    /** Adds the specified token to the list of tokens. */
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    /** Adds the specified token to the list of tokens. */
    private void addToken(TokenType type, Object literal) {
        String lexeme = source.substring(tokenStartIndex, currentIndex);
        tokens.add(new Token(type, lexeme, literal, lineNumber));
    }

    /** Returns `true` if the given character is a digit. */
    private boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }

    /** Returns `true` if the given character is an ASCII letter. */
    private boolean isAlpha(char character) {
        return (character >= 'a' && character <= 'z') ||
               (character >= 'A' && character <= 'Z') ||
               (character == '_');
    }

    /** Returns `true` if the given character is alpha or a digit. */
    private boolean isAlphaNumeric(char character) {
        return isAlpha(character) || isDigit(character);
    }

    /** Processes a string token. */
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            // Lox supports multi-line strings
            if (peek() == '\n') lineNumber++;
            consumeCharacter();
        }

        if (isAtEnd()) {
            Lox.reportError(lineNumber, "Unterminated string.");
        }

        // Closing "
        consumeCharacter();

        // Get string literal value, excluding the quotes themselves. NOTE: If Lox supported escape sequences, we would need to unescape them here.
        String value = source.substring(tokenStartIndex + 1, currentIndex - 1);
        addToken(TokenType.STRING, value);
    }

    /** Processes a number token. */
    private void number() {
        // Consume digits
        while (isDigit(peek())) consumeCharacter();

        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the . in a double/float type
            consumeCharacter();
        }

        // Consume digits
        while (isDigit(peek())) consumeCharacter();

        Double value = Double.parseDouble(source.substring(tokenStartIndex, currentIndex));
        addToken(TokenType.NUMBER, value);
    }

    /** Processes an identifier in maximal munch fashion, then treats it as either an identifier or a reserved keyword. */
    private void identifier() {
        while (isAlphaNumeric(peek())) consumeCharacter();
        String text = source.substring(tokenStartIndex, currentIndex);
        TokenType type = keywords.get(text);
        if (type == null) type = TokenType.IDENTIFIER;
        addToken(type);
    }
}
