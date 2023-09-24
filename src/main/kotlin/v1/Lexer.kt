package v1

import debug.PrintTable
import java.util.*
import kotlin.collections.Collection

open class Lexer {
    open fun analyse(sql: String): Queue<Token> = lex(sql)
}

val newLineOrTabCharSet = setOf('\n', '\r', '\t')

/**
 * Особенности:
 * - Убрать:
 * -- Все пробелы, что больше одного.
 * -- Переносы срок.
 * -- Табуляция.
 * - Обход входной строки за O(n)
 * - обойтись без RegExp
 */
fun lex(sql: String): Queue<Token> {
    val rsl = LinkedList<Token>()
    var tokenBuilder = StringBuilder()
    var prevChar = ' '
    var isCollectionStringValue = false
    sql.forEachIndexed { index, char ->
        if (prevChar == ' ' && char == prevChar || char in newLineOrTabCharSet) {
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
                println("$index + '$char'")
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

fun makeToken(tokenEndIndex: Int, tokenStr: String): Token {
    val tokenStartIndex = tokenEndIndex - (tokenStr.length - 1)
    return Token(tokenStartIndex, tokenEndIndex, tokenStr, TokenType.of(tokenStr))
}

fun makeSingleCharToken(tokenIndex: Int, tokenStr: String): Token {
    return Token(tokenIndex, tokenIndex, tokenStr, TokenType.of(tokenStr))
}

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

/**
 * testcases:
 * - "SELECT * from users;" // canonical
 * - "SELECT * from users" // no semicolon
 * - "SELECT id from users" // no semicolon + one field
 * - "SELECT id, name from users" // no semicolon + 2 fields field
 * - "SELECT users.id, users.name from users" // no semicolon + 2 fields field + table.field
 * - "SELECT u.id, u.name from users u" // no semicolon + 2 fields field + alias.field
 * - "SELECT COUNT(*) users;" // func + arg: star
 * - "SELECT COUNT(id) users;" // func + arg: column
 * - "SELECT CONCAT('Hello, ', name) users;" // func + arg: string value + column
 *
 *
 * - "SELECT    id,\r  name  \r\n  from \n users" // no semicolon + 2 fields field + space & tab chars
 */
//private val in1 = "SELECT id, name, users.email from users;"
//private val in1 = "SELECT * from users;"
//private val in1 = "SELECT \r\n*   from      users;"
//private val in1 = "SELECT u.id, u.name, COUNT(*) from users u"
private val in1 = "SELECT CONCAT('Hello, ', name) users;"

/**
 * COUNT(arg1 : columns_expression) - в том числе и просто STAR('*')
 * todo - collect string value into one token
 *      - а что если Экранирование?
 *      - вложенные Кавычки?
 */
fun main() {
    val r1 = lex(in1)
//    r1.printLexAnalyse()
//    r1.forEach{ println("${it.startIndex}, ${it.endIndex}, ${it.text}. ${it.type}, s=${in1[it.startIndex]}, e=${in1[it.endIndex]}") }
    println(r1)
}
