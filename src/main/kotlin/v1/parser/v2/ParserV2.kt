package v1.parser.v2

import v1.Lexer
import v1.Token
import v1.TokenType.*
import v1.parser.v2.ParseStrategy.ParseAnswer.*
import java.lang.System.lineSeparator

/**
 * TODO - Какие условия начала Вложенного select? - (SELECT...)
 * TableExpression == Sequence<Column>
 * ColumnExpression == Sequence<Value>
 * ValueExpression == Value
 *
 * SELECT == TableExpression
 * SELECT [column_expression_1, column_expression_2] FROM [table_expression_1, table_expression_2]
 */
class ParserV2 {

    open fun parse(tokens: List<Token>): QContainer {
        var state: ParseState? = if (tokens.first().type == SELECT) ParseState.SELECT else throw RuntimeException()

        val root = QContainer()
//        var columns = mutableListOf<ColumnExpressionTokensGroup>()
        var currColumnExpTokensGroup = TokensRange(tokens, -111)



        for (index in tokens.indices) {
            val token = tokens[index]
//            if (token.type == SPACE) continue

            when (token.type) {
                SELECT -> {
                    state = ParseState.SELECT
                    currColumnExpTokensGroup = TokensRange(tokens, index)
                    root.columns += currColumnExpTokensGroup
                    continue
                }

                FROM -> {
                    state = ParseState.FROM
                }

                COMMA -> {
                    when (state) {
                        ParseState.SELECT -> {
                            currColumnExpTokensGroup = TokensRange(tokens, index)
                            root.columns += currColumnExpTokensGroup
                            continue
                        }

                        else -> {

                        }
                    }
                }

                SPACE -> {
                    when (state) {
                        ParseState.SELECT -> {
                            currColumnExpTokensGroup.count++
                            continue
                        }

                        else -> {

                        }
                    }
                }

                else -> {}
            }


            when (state) {
                ParseState.SELECT -> {
                    currColumnExpTokensGroup.count++
                }

                else -> {
                    println("state else for ${token}, currState=$state")
                }
            }

        }

        return root
    }

    // fun scan(ASTNode : )
    /**
     * 1 - можно сразу схватывать KEYWORD для себя и отмечать флаги
     * 2 - можно создавать Ноду и возвращать её, далее она обрабатывает токены, а мы кладёмся в stack, до её return FINISH
     *      - наша ответственность знать Какие ноды доступны как НАШИ Дети
     *      - FINISH == stack.remove(currentASTNode)
     */
    open fun parseV2(analyseTokens: List<Token>): QContainer {
        val root = QContainer()
        var parserStrategy: ParseStrategy = ParseStrategy.SELECT(root, analyseTokens, 0)

        for (index in 1..<analyseTokens.size) {
            val token = analyseTokens[index]
//            (SELECT * ...)
//            SELECT * ...
            when (parserStrategy.accept(index, token, token.type)) {
                CONTINUE -> {}
                NEXT -> {}
                FINISH -> {}
                SWITCH -> {
                    parserStrategy = parseStrategy(token, root, analyseTokens, index)
                }

            }

        }
        parserStrategy.doOnFinish()

        return root
    }

    // todo - static fun candidate
    fun parseStrategy(
        token: Token,
        root: ParserV2.QContainer,
        analyseTokens: List<Token>,
        offsetTokenIndex: Int
    ) = when (token.type) {
        SELECT -> ParseStrategy.SELECT(root, analyseTokens, offsetTokenIndex)
        FROM -> ParseStrategy.FROM(root, analyseTokens, offsetTokenIndex)
        JOIN -> ParseStrategy.STUB
        else -> ParseStrategy.STUB
    }


    private enum class ParseState {
        SELECT, FROM, JOIN
    }


    public class QContainer {
        val columns: MutableList<TokensRange> = mutableListOf()
        val tables: MutableList<TokensRange> = mutableListOf()

        override fun toString(): String = columns.toString("SELECT") + tables.toString("FROM")

        private fun MutableList<TokensRange>.toString(prefix: String): String =
            if (this.isNotEmpty()) "${ls}${prefix} [${
                this.joinToString(prefix = "$ls\t", separator = ",$ls\t", postfix = ls)
            }]" else ""
    }
}

val ls: String = lineSeparator()


fun main() {
//    val input = "SELECT id, users.name FROM users"
//    val input = "SELECT id, users.name, COUNT(id) FROM users, attrs"
    val input = "SELECT id, users.name, SUBSTR('abc', 2) FROM users, attrs"
    val lexer = Lexer()
    val parser = ParserV2()
    val analyseTokens = lexer.analyse(input)
    analyseTokens.removeIf { it.type == SPACE }
    val query = parser.parseV2(analyseTokens)
    println(input)
    println((query as ParserV2.QContainer).columns.size)
    println(query)

//    val o0 = ParseStateStrategy.SELECT()
//    val o1 = ParseStateStrategy.SELECT()
}