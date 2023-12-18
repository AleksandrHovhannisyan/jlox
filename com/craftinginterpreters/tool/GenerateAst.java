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
        defineAst(outputDir, "Expr", Arrays.asList(
            "Literal  : Object value",
            "Grouping : Expr expression",
            "Binary   : Expr left, Token operator, Expr right",
            "Unary    : Token operator, Expr right"
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
        writer.println("abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);

        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }

        writer.println();
        writer.println("\tabstract <R> R accept(Visitor<R> visitor);");
        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("\tinterface Visitor<R> {");
        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("\t\tR visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
        }
        writer.println("\t}");
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
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
