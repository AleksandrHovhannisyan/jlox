package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    /** A reference to the enclosing environment. */
    final Environment parent;

    // Maps identifiers to their corresponding runtime values
    private final Map<String, Object> values = new HashMap<>();

    Environment() {
        parent = null;
    }

    Environment(Environment parent) {
        this.parent = parent;
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    void assign(Token name, Object value) {
        // Assign to a variable declared in the current scope
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }
        // Else, try assigning to a variable declared in the enclosing scope
        if (parent != null) {
            parent.assign(name, value);
            return;
        }
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    Object get(Token name) {
        // The current scope defined this identifier, so return the value
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }
        
        // Walk up the tree in case the identifier was declared in an enclosing scope
        if (parent != null) return parent.get(name);

        // "Since making it a static error makes recursive declarations too difficult, we’ll defer the error to runtime. It’s OK to refer to a variable before it’s defined as long as you don’t evaluate the reference."
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
