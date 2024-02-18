// Auto-generated from com.craftinginterpreters.tool.GenerateAst. See make ast.
package com.craftinginterpreters.lox;

import java.util.List;

/** Namespace/abstract class that defines all possible types of Exprs as a hierarchy of specialized classes. This allows us to construct an abstract syntax tree (AST) by linking instances of concrete Expr types to each other in an object-oriented manner, until we reach leaf (terminal) tokens in the tree. */
abstract class Expr {
	/** A Expr visitor must define all of the methods in this interface. */
	interface Visitor<R> {
		R visitLiteralExpr(Literal expr);
		R visitGroupingExpr(Grouping expr);
		R visitBinaryExpr(Binary expr);
		R visitUnaryExpr(Unary expr);
	}

	/** Accept a visitor to us, and instruct it on HOW to visit us so it can return a value.*/
	abstract <R> R accept(Visitor<R> visitor);

	static class Literal extends Expr {
		final Object value;

		Literal(Object value) {
			this.value = value;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitLiteralExpr(this);
		}
	}

	static class Grouping extends Expr {
		final Expr expression;

		Grouping(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitGroupingExpr(this);
		}
	}

	static class Binary extends Expr {
		final Expr left;
		final Token operator;
		final Expr right;

		Binary(Expr left, Token operator, Expr right) {
			this.left = left;
			this.operator = operator;
			this.right = right;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBinaryExpr(this);
		}
	}

	static class Unary extends Expr {
		final Token operator;
		final Expr right;

		Unary(Token operator, Expr right) {
			this.operator = operator;
			this.right = right;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitUnaryExpr(this);
		}
	}
}
