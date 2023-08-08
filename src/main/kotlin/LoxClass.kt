package com.craftinginterpreters.lox

class LoxClass(val name: String) : LoxCallable {
    override val arity: Int = 0

    override fun toString(): String {
        return name
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
        val instance = LoxInstance(this)
        return instance
    }
}