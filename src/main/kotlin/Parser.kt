package com.craftinginterpreters.lox

class Parser(private val tokens: List<Token>) {
    private var current = 0

    private class ParseError : RuntimeException()

    fun parse(): List<Stmt> {
        val statements = ArrayList<Stmt>()
        while (!isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }

        return statements
    }

    private fun expression(): Expr {
        return assignment()
    }

    private fun declaration(): Stmt? {
        try {
            if (match(TokenType.FUN)) return function("function")
            if (match(TokenType.VAR)) return varDeclaration()
            return statement()
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun statement(): Stmt {
        if (match(TokenType.PRINT)) return printStatement()
        if (match(TokenType.RETURN)) return returnStatement()
        if (match(TokenType.WHILE)) return whileStatement()
        if (match(TokenType.FOR)) return forStatement()
        if (match(TokenType.IF)) return ifStatement()
        if (match(TokenType.LEFT_BRACE)) return Stmt.Block(block())
        return expressionStatement()
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.")

        val initializer = if (match(TokenType.SEMICOLON)) {
            null
        } else if (match(TokenType.VAR)) {
            varDeclaration()
        } else {
            expressionStatement()
        }

        val condition = if (!check(TokenType.SEMICOLON)) {
            expression()
        } else {
            Expr.Literal(true)
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.")

        val increment = if (!check(TokenType.RIGHT_PAREN)) {
            expression()
        } else {
            null
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.")

        var body = statement()

        if (increment != null) {
            body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
        }

        body = Stmt.While(condition, body)

        if (initializer != null) {
            body = Stmt.Block(listOf(initializer, body))
        }

        return body
    }

    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect '(' after if condition.")

        val thenBranch = statement()
        val elseBranch = if (match(TokenType.ELSE)) {
            statement()
        } else {
            null
        }

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        val value = if (!check(TokenType.SEMICOLON)) {
            expression()
        } else {
            null
        }

        consume(TokenType.SEMICOLON, "Expect ';' after return value.")
        return Stmt.Return(keyword, value)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")

        val initializer = if (match(TokenType.EQUAL)) {
            expression()
        } else {
            null
        }

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun whileStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.")
        val body = statement()

        return Stmt.While(condition, body)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        return if (isAtEnd()) {
            Stmt.Print(expr)
        } else {
            try {
                consume(TokenType.SEMICOLON, "Expect ';' after expression.")
                Stmt.Expression(expr)
            } catch (_: ParseError) {
                Stmt.Print(expr)
            }
        }
    }

    private fun function(kind: String): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
        consume(TokenType.LEFT_PAREN, "Expect '(' after $kind name.")
        val parameters = ArrayList<Token>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(peek(), "Can't have more than 255 parameters.")
                }

                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name"))
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")

        consume(TokenType.LEFT_BRACE, "Expect '{' before $kind body.")
        val body = block()
        return Stmt.Function(name, parameters, body)
    }

    private fun block(): List<Stmt> {
        val statements = ArrayList<Stmt>()

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    private fun assignment(): Expr {
        val expr = or()

        if (match(TokenType.EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            }

            error(equals, "Invalid assignment target.")
        }

        return expr
    }


    private fun or(): Expr {
        var expr = and()

        while (match(TokenType.OR)) {
            val operator = previous()
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun and(): Expr {
        var expr = equality()

        while (match(TokenType.AND)) {
            val operator = previous()
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun equality(): Expr {
        return leftAssociativeBinaryOperator(
            ::comparison,
            TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL,
        )
    }

    private fun leftAssociativeBinaryOperator(operandMethod: () -> Expr, vararg types: TokenType): Expr {
        var expr = operandMethod()

        while (match(*types)) {
            val operator = previous()
            val right = operandMethod()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd(): Boolean {
        return peek().type == TokenType.EOF
    }

    private fun peek(): Token {
        return tokens[current]
    }

    private fun previous(): Token {
        return tokens[current - 1]
    }

    private fun error(token: Token, message: String): ParseError {
        com.craftinginterpreters.lox.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return

            when (peek().type) {
                TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR, TokenType.IF,
                TokenType.WHILE, TokenType.PRINT, TokenType.RETURN,
                -> return
                else -> advance()
            }
        }
    }

    private fun comparison(): Expr {
        return leftAssociativeBinaryOperator(
            ::term,
            TokenType.GREATER,
            TokenType.GREATER_EQUAL,
            TokenType.LESS,
            TokenType.LESS_EQUAL
        )
    }

    private fun term(): Expr {
        return leftAssociativeBinaryOperator(::factor, TokenType.MINUS, TokenType.PLUS)
    }

    private fun factor(): Expr {
        return leftAssociativeBinaryOperator(::unary, TokenType.SLASH, TokenType.STAR)
    }

    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return call()
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments = ArrayList<Expr>()

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) {
                    error(peek(), "Can't have more than 255 arguments.")
                }
                arguments.add(expression())
            } while (match(TokenType.COMMA))
        }

        val paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")

        return Expr.Call(callee, paren, arguments)
    }


    private fun call(): Expr {
        var expr = primary()

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr)
            } else {
                break
            }
        }

        return expr
    }


    private fun primary(): Expr {
        return when {
            match(TokenType.FALSE) -> Expr.Literal(false)
            match(TokenType.TRUE) -> Expr.Literal(true)
            match(TokenType.NIL) -> Expr.Literal(null)
            match(TokenType.NUMBER, TokenType.STRING) -> Expr.Literal(previous().literal)
            match(TokenType.LEFT_PAREN) -> {
                val expr = expression()
                consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
                Expr.Grouping(expr)
            }
            match(TokenType.IDENTIFIER) -> Expr.Variable(previous())
            else -> throw error(peek(), "Expect expression.")
        }
    }


}