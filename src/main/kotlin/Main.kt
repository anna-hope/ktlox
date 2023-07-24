package com.craftinginterpreters.lox

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

private var hadError = false

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
}

private fun runPrompt() {
    while (true) {
        print("> ")
        val line = readln()
        run(line)
        hadError = false
    }
}

private fun run(source: String) {
    val scanner = Scanner(source)
    val tokens = scanner.scanTokens()

    for (token in tokens) {
        println(token)
    }
}

fun error(line: Int, message: String) {
    report(line, "", message)
}

private fun report(line: Int, where: String, message: String) {
    System.err.println("[line: $line] Error$where: $message")
    hadError = true
}