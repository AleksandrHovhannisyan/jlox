package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        // Expressions
        defineAst(outputDir, "Expr", Arrays.asList(
            // Literal (e.g., 4, "string", true, false, nil)
            "Literal    : Object value",
            // Grouping (e.g., 2 * (3 - 1))
            "Grouping   : Expr expression",
            // Binary (e.g., 1 + 2)
            "Binary     : Expr left, Token operator, Expr right",
            // Unary (e.g., !true, -1)
            "Unary      : Token operator, Expr right",
            // Variable access (e.g., identifier, print identifier)
            "Variable   : Token name",
            // Assignment (e.g., a = 2)
            "Assignment : Token name, Expr value",
            // Logical
            "Logical    : Expr left, Token operator, Expr right"
        ));
        // Statements
        defineAst(outputDir, "Stmt", Arrays.asList(
            // Block statements
            "Block       : List<Stmt> statements",
            // Expression statement
            "Expression  : Expr expression",
            // Print statement
            "Print       : Expr expression",
            // Variable declarations
            "Var         : Token name, Expr initializer",
            // If statements
            "If          : Expr condition, Stmt thenBranch, Stmt elseBranch"
        ));
    }

    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
       String path = outputDir + "/" + baseName + ".java";
       PrintWriter writer = new PrintWriter(path, "UTF-8");

       writer.println("// Auto-generated from com.craftinginterpreters.tool.GenerateAst. See make ast.");
        writer.println("package com.craftinginterpreters.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("/** Namespace/abstract class that defines all possible types of " + baseName + "s as a hierarchy of specialized classes. This allows us to construct an abstract syntax tree (AST) by linking instances of concrete " + baseName + " types to each other in an object-oriented manner, until we reach leaf (terminal) tokens in the tree. */");
        writer.println("abstract class " + baseName + " {");

        defineVisitorInterface(writer, baseName, types);

        writer.println();
        writer.println("\t/** Accept a visitor to us, and instruct it on HOW to visit us so it can return a value.*/");
        writer.println("\tabstract <R> R accept(Visitor<R> visitor);");

        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineConcreteClass(writer, baseName, className, fields);
        }

        writer.println("}");
        writer.close();
    }

    private static void defineVisitorInterface(PrintWriter writer, String baseName, List<String> types) {
        writer.println("\t/** A " + baseName + " visitor must define all of the methods in this interface. */");
        writer.println("\tinterface Visitor<R> {");
        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("\t\tR visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
        }
        writer.println("\t}");
    }

    private static void defineConcreteClass(PrintWriter writer, String baseName, String className, String fieldList) {
        writer.println();
        writer.println("\tstatic class " + className + " extends " + baseName + " {");
        String[] fields = fieldList.split(", ");

        // Field declarations
        for (String field : fields) {
            writer.println("\t\tfinal " + field + ";");
        }

        // Constructor
        writer.println();
        writer.println("\t\t" + className + "(" + fieldList + ") {");
        for (String field : fields) {
            String fieldName = field.split(" ")[1];
            writer.println("\t\t\tthis." + fieldName + " = " + fieldName + ";");
        }
        writer.println("\t\t}");

        // Visitor pattern
        writer.println();
        writer.println("\t\t@Override");
        writer.println("\t\t<R> R accept(Visitor<R> visitor) {");
        writer.println("\t\t\treturn visitor.visit" + className + baseName + "(this);");
        writer.println("\t\t}");

        writer.println("\t}");
    }
}
