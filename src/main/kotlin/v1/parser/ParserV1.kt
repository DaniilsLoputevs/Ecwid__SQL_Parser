package v1.parser

import v1.Token
import v1.TokenType.*
import java.util.*

/**
 * TODO - Какие условия начала Вложенного select? - (SELECT...)
 */
class ParserV1 {
    open fun parse(analyse: Deque<Token>): Any? {
        val unBuildQueries = mutableListOf<UnBuildQueryContainer>()

        val root = Query()
        var currQuery: Query = root


        var parseState : ParseState
        var currBuilder : ExpressionBuilder = StubExpressionBuilder
        var collectList : MutableList<SyntaxNode> = mutableListOf() // реально, в этот лист обязан заменится быстрее чем, в него что-то упадёт.

        var prevToken = analyse.poll()
        var token = analyse.poll()
        while (token != null) {
            if (token.type == SPACE) continue

            // такая ситуация значит что мы нашли вложенный SELECT в любом месте дерева...
            if (prevToken.type == L_PAR && token.type == SELECT) {
                collectUnBuildQuery(analyse).also { unBuildQuery ->
//                    val collectList = root.columns // todo make it state-machine
                    collectList.add(unBuildQuery)
                    unBuildQueries.add(UnBuildQueryContainer(collectList, collectList.lastIndex, unBuildQuery))
                }
            }

            when (token.type) {
                SELECT -> {
                    currBuilder = ColumnExpressionBuilder()
                    collectList = currQuery.columns
                    parseState = ParseState.SELECT
                }
                FROM -> {
                    currBuilder = ColumnExpressionBuilder()
                    collectList = currQuery.froms
                    parseState = ParseState.FROM
                }
//                FROM -> currBuilder = ColumnExpressionBuilder()
                JOIN -> {
                    currBuilder = ColumnExpressionBuilder()
                    collectList = currQuery.joins
                    parseState = ParseState.JOIN
                }
                COMMA -> currBuilder.build().apply(collectList::add)
                else -> {

                }
            }

        }


        return null
    }

    private fun collectUnBuildQuery(analyse: Deque<Token>): UnBuildQuerySyntaxNode {
        if (analyse.isEmpty()) throw RuntimeException("Excepted not empty \$analyse")
        if (analyse.peekFirst().type != L_PAR) throw RuntimeException(
            "Expected nested select query that start from Token(type=L_PAR) " +
                    "but found ${analyse.peekFirst().type}"
        )
        val lexStack = LinkedList<Token>()
        var left = 1
        var right = 0
        var token = analyse.poll()
        while (token != null) {
            if (right == left) break
            when (token.type) {
                L_PAR -> left++
                R_PAR -> right++
                else -> lexStack.offer(token)
            }
            token = analyse.poll()
        }
        return UnBuildQuerySyntaxNode(lexStack)
    }
    private enum class ParseState {SELECT, FROM, JOIN}
}