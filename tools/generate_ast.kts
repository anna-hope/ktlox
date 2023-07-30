import java.io.PrintWriter
import kotlin.system.exitProcess

fun generateAst(args: Array<String>) {
    if (args.size != 1) {
        println("Usage: generate_ast <output directory>")
        exitProcess(64)
    }
    val outputDir = args[0]
    defineAst(
        outputDir, "Expr", listOf(
            "Binary : val left: Expr, val operator: Token, val right: Expr",
            "Grouping : val expression: Expr", "Literal : val value: Any?",
            "Unary : val operator: Token, val right: Expr",
            "Variable : val name: Token",
        )
    )
    defineAst(
        outputDir, "Stmt", listOf(
            "Expression : val expression: Expr",
            "Print : val expression: Expr",
            "Var : val name: Token, val initializer: Expr?",
        )
    )
}

fun defineAst(outputDir: String, baseName: String, types: List<String>) {
    val path = "$outputDir/$baseName.kt"
    with(PrintWriter(path, "UTF-8")) {
        println("package com.craftinginterpreters.lox")
        println()
        println("abstract class $baseName {")
        defineVisitor(this, baseName, types)
        println()

        // AST classes.
        for (type in types) {
            val className = type.split(":")[0].trim()

            // Set limit to 2 to avoid splitting on type annotation.
            val fields = type.split(":", limit = 2)[1].trim()
            defineType(this, baseName, className, fields)
            println()
        }
        println()

        println("   abstract fun <R> accept(visitor: Visitor<R>) : R")

        println("}")
        close()
    }
}

fun defineVisitor(writer: PrintWriter, baseName: String, types: List<String>) {
    with(writer) {
        println("   interface Visitor<R> {")
        for (type in types) {
            val typeName = type.split(":")[0].trim()
            println(
                "       fun visit$typeName$baseName(${baseName.lowercase()}: $typeName" +
                        "): R"
            )
        }
        println("   }")
    }
}

fun defineType(writer: PrintWriter, baseName: String, className: String, fieldList: String) {
    with(writer) {
        println("   class $className($fieldList) : $baseName() {")

        // Visitor pattern.
        println()
        println("       override fun <R> accept(visitor: Visitor<R>): R {")
        println("           return visitor.visit$className$baseName(this)")
        println("       }")
        println("   }")
    }
}

generateAst(args)