package v1

import java.util.*


data class Token(
    /** Индекс первого символа Токена.text в изначальном sql.
     * Пример Для "select" из самого начала sql, значение == 0 */
    val startIndex: Int,
    /** Индекс последнего символа Токена.text в изначальном sql.
     * Пример Для "select" из самого начала sql, значение == 5 */
    val endIndex: Int,
    val text: String,
    val type: TokenType
)

enum class TokenType {
    SELECT, STAR, FROM, AS,
    LEFT, RIGHT, INNER, OUTER, FULL, JOIN, ON,
    GROUP, BY, HAVING,
    LIMIT, OFFSET,
    AND, OR,
    L_PAR, R_PAR,
    PLUS, MINUS, EQUALS, LESS, MORE,

    SINGLE_QUOTE,
    DOT, COMMA, SPACE,
    SEMICOLON,
    EXPRESSION; // ? STRING, NUMBER - оно нужно? в целом можно парсить отдельно... даже с number literals

    companion object {
        fun of(str: String): TokenType = when (str) {
            "select", "SELECT" -> SELECT
            "*" -> STAR
            "from", "FROM" -> FROM
            "as", "AS" -> AS
            "left", "LEFT" -> LEFT
            "right", "RIGHT" -> RIGHT
            "inner", "INNER" -> INNER
            "outer", "OUTER" -> OUTER
            "full", "FULL" -> FULL
            "join", "JOIN" -> JOIN
            "on", "ON" -> ON
            "group", "GROUP" -> GROUP
            "by", "BY" -> BY
            "having", "HAVING" -> HAVING
            "limit", "LIMIT" -> LIMIT
            "offset", "OFFSET" -> OFFSET
            "and", "AND" -> AND
            "or", "OR" -> OR
            "(" -> L_PAR
            ")" -> R_PAR
            "+" -> PLUS
            "-" -> MINUS
            "=" -> EQUALS
            "<" -> LESS
            ">" -> MORE
            "'" -> SINGLE_QUOTE
            "." -> DOT
            "," -> COMMA
            " " -> SPACE
            ";" -> SEMICOLON
            else -> EXPRESSION
        }
    }
}


open class Lexer {

    private val controlChars = setOf('\n', '\r', '\t', '\b')

    /**
     * Особенности:
     * - Убрать:
     * -- Все пробелы, что больше одного.
     * -- Переносы срок.
     * -- Табуляция.
     * - Обход входной строки за O(n)
     * - обойтись без RegExp
     */
    open fun analyse(sql: String): Queue<Token> {
        val rsl = LinkedList<Token>()
        var tokenBuilder = StringBuilder()
        var prevChar = ' '
        var isCollectionStringValue = false
        sql.forEachIndexed { index, char ->
            // пропускаем Control chars ['\n', '\r', '\t', '\b', \r\n]
            if (char in controlChars) return@forEachIndexed // forEach continue

            // 2 и более пробелов - должны считаться за 1 Token(type = SPACE)
            if (prevChar == ' ' && char == prevChar) {
                prevChar = char
                return@forEachIndexed // forEach continue
            }

            // конец сбора string value в 1 token
            if (isCollectionStringValue) {
                if (char == '\'' && prevChar != '\\') {
                    tokenBuilder.append(char)
                    rsl += makeToken(index, tokenBuilder.toString())
                    tokenBuilder = StringBuilder()
                    isCollectionStringValue = false

                } else
                    tokenBuilder.append(char)

                return@forEachIndexed // forEach continue
            }

            when (char) {
                ' ', ',', ';', '(', ')' -> {
//                println("$index + '$char'")
                    if (tokenBuilder.isNotEmpty()) {
                        rsl += makeToken(index - 1, tokenBuilder.toString())
                        tokenBuilder = StringBuilder()
                    }
                    rsl += makeSingleCharToken(index, char.toString())
                }

                // начало сбора string value в 1 token
                '\'' -> {
                    isCollectionStringValue = true
                    tokenBuilder.append(char)
                }

                else -> tokenBuilder.append(char)
            }
            prevChar = char
        }

        if (tokenBuilder.isNotEmpty()) // на случай когда в конце sql НЕТ SEMICOLON
            rsl += makeToken(sql.length - 1, tokenBuilder.toString())

        return rsl
    }

    private fun makeToken(tokenEndIndex: Int, tokenStr: String): Token {
        val tokenStartIndex = tokenEndIndex - (tokenStr.length - 1)
        return Token(tokenStartIndex, tokenEndIndex, tokenStr, TokenType.of(tokenStr))
    }

    private fun makeSingleCharToken(tokenIndex: Int, tokenStr: String): Token {
        return Token(tokenIndex, tokenIndex, tokenStr, TokenType.of(tokenStr))
    }

}
