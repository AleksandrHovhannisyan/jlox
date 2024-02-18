// Auto-generated from com.craftinginterpreters.tool.GenerateAst. See make ast.
package com.craftinginterpreters.lox;

import java.util.List;

/** Namespace/abstract class that defines all possible types of Stmts as a hierarchy of specialized classes. This allows us to construct an abstract syntax tree (AST) by linking instances of concrete Stmt types to each other in an object-oriented manner, until we reach leaf (terminal) tokens in the tree. */
abstract class Stmt {
	/** A Stmt visitor must define all of the methods in this interface. */
	interface Visitor<R> {
		R visitExpressionStmt(Expression stmt);
		R visitPrintStmt(Print stmt);
	}

	/** Accept a visitor to us, and instruct it on HOW to visit us so it can return a value.*/
	abstract <R> R accept(Visitor<R> visitor);

	static class Expression extends Stmt {
		final Expr expression;

		Expression(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitExpressionStmt(this);
		}
	}

	static class Print extends Stmt {
		final Expr expression;

		Print(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitPrintStmt(this);
		}
	}
}
