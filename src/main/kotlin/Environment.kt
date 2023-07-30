package com.craftinginterpreters.lox

class Environment {
    private val values = HashMap<String, Any?>()

    fun get(name: Token): Any? {
        if (name.lexeme in values) {
            return values[name.lexeme]
        }

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun define(name: String, value: Any?) {
        values[name] = value
    }
}