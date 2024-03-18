package com.craftinginterpreters.lox;

import java.util.List;

interface LoxCallable {
    int arity();

    /* From the book: We pass in the interpreter in case the class implementing call() needs it. 
     * We also give it the list of evaluated argument values. The implementer’s job is then to 
     * return the value that the call expression produces. */
    Object call(Interpreter interpreter, List<Object> arguments);
}
