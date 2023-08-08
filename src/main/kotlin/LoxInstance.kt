package com.craftinginterpreters.lox

class LoxInstance(private val klass: LoxClass) {
    private val fields = HashMap<String, Any?>()

    operator fun get(name: Token): Any? {
        if (name.lexeme in fields) {
            return fields[name.lexeme]
        }

        throw RuntimeError(name, "Undefined property ${name.lexeme}.")
    }

    operator fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }

    override fun toString(): String {
        return "${klass.name} instance"
    }
}