package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
  // Maps identifiers to their corresponding runtime values
  private final Map<String, Object> values = new HashMap<>();

  void define(String name, Object value) {
      values.put(name, value);
  }

  Object get(Token name) {
      if (values.containsKey(name.lexeme)) {
          return values.get(name.lexeme);
      }
      // "Since making it a static error makes recursive declarations too difficult, we’ll defer the error to runtime. It’s OK to refer to a variable before it’s defined as long as you don’t evaluate the reference."
      throw new RuntimeError(name,
        "Undefined variable '" + name.lexeme + "'.");
  }
}
