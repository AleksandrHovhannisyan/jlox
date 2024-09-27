package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;

    LoxFunction(Stmt.Function declaration) {
        this.declaration = declaration;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        // Each function CALL needs its own environment (stack frame)
        Environment environment = new Environment(interpreter.globals);
        for (int i = 0; i < declaration.parameters.size(); i++) {
            // Bind to each parameter of this function call the corresponding value from the arguments list
            environment.define(declaration.parameters.get(i).lexeme, arguments.get(i));
        }
        interpreter.executeBlock(declaration.body, environment);
        return null;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
