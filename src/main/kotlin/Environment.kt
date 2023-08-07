package com.craftinginterpreters.lox

class Environment(val enclosing: Environment? = null) {
    private val values = HashMap<String, Any?>()

    operator fun get(name: Token): Any? {
        if (name.lexeme in values) return values[name.lexeme]

        if (enclosing != null) return enclosing[name]

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

    fun ancestor(distance: Int): Environment {
        var environment = this
        for (i in 0 until distance) {
            // We know the property will be there because the resolver already
            // found it before.
            environment = environment.enclosing!!
        }

        return environment
    }

    fun getAt(distance: Int, name: String): Any? {
        return ancestor(distance).values[name]
    }
}