package com.craftinginterpreters.lox

class LoxClass(val name: String, private val methods: Map<String, LoxFunction>) : LoxCallable {
    override val arity: Int
        get() {
            val initializer = findMethod("init")
            return initializer?.arity ?: 0
        }

    fun findMethod(name: String): LoxFunction? {
        return methods[name]
    }

    override fun toString(): String {
        return name
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
        val instance = LoxInstance(this)
        val initializer = findMethod("init")
        initializer?.bind(instance)?.call(interpreter, arguments)
        return instance
    }
}