package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadSyntaxError = false;
    static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    /** Runs Lox interpreter on an input file */
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadSyntaxError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    /** Runs Lox interpreter as a REPL */
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadSyntaxError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Early-return if there was a syntax error
        if (hadSyntaxError) return;
        // Otherwise interpret it
        interpreter.interpret(statements);
    }

    static void reportError(int lineNumber, String message) {
        reportSyntaxError(lineNumber, "", message);
    }

    static void reportError(Token token, String message) {
        if (token.type == TokenType.EOF) {
            reportSyntaxError(token.lineNumber, " at end", message);
        } else {
            reportSyntaxError(token.lineNumber, " at '" + token.lexeme + "'", message);
        }
    }

    static void reportRuntimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
                "\n[line " + error.token.lineNumber + "]");
        hadRuntimeError = true;
    }

    private static void reportSyntaxError(int lineNumber, String where, String message) {
        System.err.println("[line " + lineNumber + "] Error" + where + ": " + message);
        hadSyntaxError = true;
    }
}
