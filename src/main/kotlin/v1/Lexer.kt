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
    /* KEYWORDS */
    SELECT, DISTINCT, ALL, STAR, AS,
    FROM, LATERAL,
    LEFT, RIGHT, INNER, OUTER, FULL, CROSS, JOIN, ON,
    WHERE,
    GROUP, BY,
    HAVING,
    ORDER, ASC, DESC, USING,
    LIMIT,
    OFFSET, ROW, ROWS,
    FETCH, FIRST, NEXT, ONLY,
    FOR, // SELECT FOR UPDATE
    NO, KEY, UPDATE, // единое выражение для SELECT FOR UPDATE - NO KEY UPDATE
    SHARE, // единое выражение для SELECT FOR UPDATE - KEY SHARE || SHARE
    OF, // SELECT OF ${table_expression}
    NOWAIT,
    SKIP, LOCKED, // единое выражение для SELECT FOR UPDATE - SKIP LOCKED


    /* OPERATORS */
    PLUS, MINUS,
    DIVIDE, // MULTIPLY == STAR
    MODULO, EXPONENT,
    NOT, AND, OR,
    EQUALS, LESS, MORE, LESS_OR_EQUALS, MORE_OR_EQUALS,
    BETWEEN, IN, LIKE, ILIKE, SIMILAR,

    /* SYMBOLS */
    L_PAR, R_PAR, L_SQUARE_PAR, R_SQUARE_PAR,
    SINGLE_QUOTE, DOUBLE_QUOTE,
    DOT, COMMA, SPACE,
    SEMICOLON,

    /* VALUE */
    CONSTANT,
    EXPRESSION; // ? CONSTANT, STRING, NUMBER - оно нужно? в целом можно парсить отдельно... даже с number literals

    companion object {
        /**
         * Примечание 1 - верим в порядочность юзеров, что они не станут писать keyword так "SeLeCt * fRoM users".
         * Примечание 2 - доверяй, но проверяй - проверяем кейс когда keyword имеет разные кейсы.
         * Примечание 3 - если это не что-то из [KEYWORDS, OPERATORS, SYMBOLS] в любых его допустимо-возможных формах,
         *                  то это что-то из EXPRESSION [constant, func, table, column, ...].
         */
        fun of(str: String): TokenType {
            var rsl: TokenType? = parseConstantElseNull(str)
            if (rsl != null) return rsl

            rsl = parseKeywordOrOperatorsOrSymbolElseNull(str) // примечание 1
            if (rsl != null) return rsl

            rsl = parseKeywordOrOperatorsOrSymbolElseNull(str.uppercase(Locale.getDefault())) // примечание 2
            if (rsl != null) return rsl
            return EXPRESSION // Примечание 3
        }

        val keywords: Set<TokenType> = setOf(
            SELECT, DISTINCT, ALL, STAR, AS,
            FROM, LATERAL,
            LEFT, RIGHT, INNER, OUTER, FULL, CROSS, JOIN, ON,
            WHERE,
            GROUP, BY,
            HAVING,
            ORDER, ASC, DESC, USING,
            LIMIT,
            OFFSET, ROW, ROWS,
            FETCH, FIRST, NEXT, ONLY,
            FOR, NO, KEY, UPDATE, SHARE, OF, NOWAIT, SKIP, LOCKED,
        )
        val operators: Set<TokenType> = setOf(
            PLUS, MINUS,
            DIVIDE, // MULTIPLY == STAR
            MODULO, EXPONENT,
            NOT, AND, OR,
            EQUALS, LESS, MORE, LESS_OR_EQUALS, MORE_OR_EQUALS,
            BETWEEN, IN, LIKE, ILIKE, SIMILAR,
        )
        val symbols: Set<TokenType> = setOf(
            L_PAR, R_PAR, L_SQUARE_PAR, R_SQUARE_PAR,
            SINGLE_QUOTE, DOUBLE_QUOTE,
            DOT, COMMA, SPACE,
            SEMICOLON,
        )

        private fun parseConstantElseNull(str: String): TokenType? = when {
            str.startsWith('\'') -> CONSTANT //  Text value
            str[0].isDigit() -> CONSTANT //  Number value
            str.length == 4 && str.equals("true", ignoreCase = true) -> CONSTANT
            str.length == 5 && str.equals("false", ignoreCase = true) -> CONSTANT
            else -> null
        }

        private fun parseKeywordOrOperatorsOrSymbolElseNull(str: String): TokenType? = when (str) {
            "select", "SELECT" -> SELECT
            "distinct", "DISTINCT" -> DISTINCT
            "all", "ALL" -> ALL
            "*" -> STAR
            "from", "FROM" -> FROM
            "lateral", "LATERAL" -> LATERAL
            "as", "AS" -> AS

            "left", "LEFT" -> LEFT
            "right", "RIGHT" -> RIGHT
            "inner", "INNER" -> INNER
            "outer", "OUTER" -> OUTER
            "full", "FULL" -> FULL
            "cross", "CROSS" -> CROSS
            "join", "JOIN" -> JOIN
            "on", "ON" -> ON

            "where", "WHERE" -> WHERE

            "group", "GROUP" -> GROUP
            "by", "BY" -> BY
            "order", "ORDER" -> ORDER
            "asc", "ASC" -> ASC
            "desc", "DESC" -> DESC
            "using", "USING" -> USING

            "having", "HAVING" -> HAVING
            "limit", "LIMIT" -> LIMIT
            "offset", "OFFSET" -> OFFSET
            "row", "ROW" -> ROW
            "rows", "ROWS" -> ROWS

            "fetch", "FETCH" -> FETCH
            "first", "FIRST" -> FIRST
            "next", "NEXT" -> NEXT
            "only", "ONLY" -> ONLY

            "for", "FOR" -> FOR
            "no", "NO" -> NO
            "key", "KEY" -> KEY
            "update", "UPDATE" -> UPDATE
            "share", "SHARE" -> SHARE
            "of", "OF" -> OF
            "nowait", "NOWAIT" -> NOWAIT
            "skip", "SKIP" -> SKIP
            "locked", "LOCKED" -> LOCKED

            "+" -> PLUS
            "-" -> MINUS
            "/" -> DIVIDE
            "%" -> MODULO
            "^" -> EXPONENT

            "not", "NOT" -> NOT
            "and", "AND" -> AND
            "or", "OR" -> OR

            "=" -> EQUALS
            "<" -> LESS
            ">" -> MORE
            "<=" -> LESS_OR_EQUALS
            ">=" -> MORE_OR_EQUALS

            "between", "BETWEEN" -> BETWEEN
            "in", "IN" -> IN
            "like", "LIKE" -> LIKE
            "ilike", "ILIKE" -> ILIKE
            "similar", "SIMILAR" -> SIMILAR

            "(" -> L_PAR
            ")" -> R_PAR
            "[" -> L_SQUARE_PAR
            "]" -> R_SQUARE_PAR
            "'" -> SINGLE_QUOTE
            "\"" -> DOUBLE_QUOTE
            "." -> DOT
            "," -> COMMA
            " " -> SPACE
            ";" -> SEMICOLON
            else -> null
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
     * @param isSkipSpaces не уверен что ПРОБЕЛЫ совершенно бесполезны, если это так то
     *                      todo - если бесполезны -> выпилить пробелы, а не как заглушка
     */
    open fun analyse(sql: String, isSkipSpaces: Boolean = false): LinkedList<Token> {
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

        if (isSkipSpaces) rsl.removeIf { it.type == TokenType.SPACE }
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
