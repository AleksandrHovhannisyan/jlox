package com.craftinginterpreters.lox;

class Interpreter implements Expr.Visitor<Object> {

    public void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
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
}
