build: $(shell find com/craftinginterpreters/lox -type f)
	javac com/craftinginterpreters/lox/Lox.java

ast: com/craftinginterpreters/tool/GenerateAst.java
	javac com/craftinginterpreters/tool/GenerateAst.java && java com.craftinginterpreters.tool.GenerateAst com/craftinginterpreters/lox

run:
	java com.craftinginterpreters.lox.Lox $(FILE)

clean:
	rm com/craftinginterpreters/lox/*.class
