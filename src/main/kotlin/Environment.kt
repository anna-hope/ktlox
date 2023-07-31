package com.craftinginterpreters.lox

class Environment(val enclosing: Environment? = null) {
    private val values = HashMap<String, Any?>()

    fun get(name: Token): Any? {
        if (name.lexeme in values) return values[name.lexeme]

        if (enclosing != null) return enclosing.get(name)

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun assign(name: Token, value: Any?) {
        if (name.lexeme in values) {
            values[name.lexeme] = value
        } else if (enclosing != null) {
            enclosing.assign(name, value)
        } else {
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }

    }

    fun define(name: String, value: Any?) {
        values[name] = value
    }
}