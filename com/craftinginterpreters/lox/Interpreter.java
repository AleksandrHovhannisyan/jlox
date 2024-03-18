package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    // Every language has a global scope (e.g., JavaScript has window, Node has global, etc.)
    private Environment globals = new Environment();
    private Environment environment = globals;

    Interpreter() {
        // Define globals for Lox
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }

    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.reportRuntimeError(error);
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }
    
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }
    
    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        // Notice how unary expressions being right-associative translates naturally into the code first evaluating
        // the right side of the expression and THEN deciding how to apply the operator.
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            case BANG:
                return !isTruthy(right);
        }

        // Unreachable but needed to satisfy compiler
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }
    
        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.closingParenthesis, "Can only call functions and classes.");
        }
       
        LoxCallable function = (LoxCallable)callee;

        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.closingParenthesis, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        
        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                checkDivisionByZero(expr.operator, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case PLUS: {
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            }
        }

        // Unreachable
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    @Override
    public Object visitAssignmentExpr(Expr.Assignment expr) {
        Token name = expr.name;
        Object value = evaluate(expr.value);
        environment.assign(name, value);
        return value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }
        return evaluate(expr.right);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression statement) {
        evaluate(statement.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print statement) {
        Object value = evaluate(statement.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var statement) {
        Object value = null;
        if (statement.initializer != null) {
            value = evaluate(statement.initializer);
        }
        environment.define(statement.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block statement) {
        executeBlock(statement.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If statement) {
        Object condition = evaluate(statement.condition);
        if (isTruthy(condition)) {
            execute(statement.thenBranch);
        } else if (statement.elseBranch != null) {
            execute(statement.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While statement) {
        while (isTruthy(evaluate(statement.condition))) {
            execute(statement.body);
        }
        return null;
    }

    /** Evaluates the given expression, returning an Object representing the result. */
    private Object evaluate(Expr expression) {
        return expression.accept(this);
    }

    /** Returns `true` if the given object is truthy in Lox. To quote the book: 
     * "Lox follows Ruby’s simple rule: false and nil are falsey, and everything else is truthy."
     */
    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    /** Returns `true` if the two values and types are equal and `false` otherwise. */
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    /** Checks that the given unary operand is a number. If it isn't, throws an error. */
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    /** Checks that the given binary operands are both numbers. If they aren't, throws an error. */
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    /** Checks for division by zero. See Chapter 7 challenge 3. */
    private void checkDivisionByZero(Token operator, Object right) {
        if (right instanceof Double && (double)right == 0.0) {
            throw new RuntimeError(operator, "Cannot divide by zero.");
        }
    }

    /** Returns a string representing the Lox equivalent of a Java runtime object */
    private String stringify(Object object) {
        if (object == null) return "nil";

        /* Quote from the book: "Lox uses double-precision numbers even for integer values. 
         * In that case, they should print without a decimal point. Since Java has both floating point 
         * and integer types, it wants you to know which one you’re using. It tells you by adding an 
         * explicit .0 to integer-valued doubles. We don’t care about that, so we hack it off the end." */
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        // Everything else can just be stringified directly
        return object.toString();
    }


    /** Executes a given block statement in the specified environment. */
    public void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            // Create a new environment for the block
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            // Restore old environment (allows environments to have multiple block children)
            this.environment = previous;
        }
    }

    /** Executes a statement. */
    private void execute(Stmt statement) {
        statement.accept(this);
    }
}
