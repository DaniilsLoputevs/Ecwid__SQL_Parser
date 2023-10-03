package v1.parser

import v1.Token
import java.util.*


class ColumnExpressionBuilder : ExpressionBuilder{
    val expression = ColumnExpression()
    override fun accept(token: Token) : Boolean {
        return false
    }

    override fun build(): SyntaxNode = expression
}
object StubExpressionBuilder : ExpressionBuilder {
    override fun accept(token: Token): Boolean {
        TODO("Not yet implemented")
    }

    override fun build(): SyntaxNode {
        TODO("Not yet implemented")
    }

}
interface ExpressionBuilder {
    fun accept(token: Token) : Boolean
    fun build() : SyntaxNode
}


data class UnBuildQueryContainer(
    val place: MutableList<SyntaxNode>,
    val indexInPlace: Int,
    val unBuildQuery: UnBuildQuerySyntaxNode
)

interface SyntaxNode {
    val text: String
}

class Query : SyntaxNode {
    val columns: MutableList<SyntaxNode> = mutableListOf()
    val froms: MutableList<SyntaxNode> = mutableListOf()
    val joins: MutableList<SyntaxNode> = mutableListOf()
    override val text: String
        get() = "SELECT ${columns.joinToString(", ")} " +
                "${fromsToString()} "

    private fun fromsToString(): String = if (froms.isNotEmpty()) "FROM " + froms.joinToString(",") else " "

}

class ColumnExpression : SyntaxNode {
    var column: Token? = null // todo is it good to be nullable?
    var alias: Token? = null

    override val text: String
        get() = TODO("Not yet implemented")

}

class UnBuildQuerySyntaxNode(val lexStack: Deque<Token>) : SyntaxNode {

    override val text: String
        get() = lexStack.joinToString(",")

}