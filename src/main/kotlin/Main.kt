package com.craftinginterpreters.lox

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

private val interpreter = Interpreter()
var hadError = false
var hadRuntimeError = false

fun main(args: Array<String>) {
    if (args.size > 1) {
        println("Usage: ktlox [script]")
    } else if (args.size == 1) {
        runFile(args[0])
    } else {
        runPrompt()
    }
}

private fun runFile(path: String) {
    val bytes = Files.readAllBytes(Paths.get(path))
    run(String(bytes))

    if (hadError) {
        exitProcess(65)
    }

    if (hadRuntimeError) {
        exitProcess(70)
    }
}

private fun runPrompt() {
    while (true) {
        print("> ")
        val line = readln()
        if (line.isNotEmpty()) {
            try {
                run(line)
            } catch (_: RuntimeException) {
                // We need this to not crash on ParseError.
            }
            hadError = false
        }
    }
}

private fun run(source: String) {
    val scanner = Scanner(source)
    val tokens = scanner.scanTokens()

    val parser = Parser(tokens)
    val statements = parser.parse()

    if (hadError) return

    val resolver = Resolver(interpreter)
    resolver.resolve(statements)

    if (hadError) return

    interpreter.interpret(statements)
}

fun error(line: Int, message: String) {
    report(line, "", message)
}

private fun report(line: Int, where: String, message: String) {
    System.err.println("[line: $line] Error$where: $message")
    hadError = true
}

fun error(token: Token, message: String) {
    when (token.type) {
        TokenType.EOF -> report(token.line, " at end", message)
        else -> report(token.line, " at '${token.lexeme}'", message)
    }
}

fun runtimeError(error: RuntimeError) {
    System.err.println("${error.message}\n[line ${error.token.line}]")
    hadRuntimeError = true
}