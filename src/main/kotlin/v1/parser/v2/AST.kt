import v1.Token
import v1.parser.v2.TokensRange
import java.util.*
import kotlin.reflect.KClass

interface ASTNode {}

/**
 * Некоторые реализации [ASTNode] могут иметь Алиасы. Интерфейс описывает их работу.
 * В случаи когда Алиас, может быть, но его нет, Property alias должно быть равно Пустой строке("")
 */
interface Alias {
    var alias: String
}

/**
 * Успех в понимании этой абстракции лежит в понимании, что [ExpressionNode] это штука, выполнив в реальном sql,
 * мы получим value. Короче говоря, это что-то, что имеет return value
 */
interface ExpressionNode : ASTNode

/**
 * Т.к. в ходе парсинга списка токена в AST, мы можем встретить рекурсивный момент, например:
 * - select в select | select (select id from posts), users.id from users
 * - expression с разбросанным приоритетом операторов | users.age > 10 * (2 + ageGroup) && users.age < 10 * (5 + ageGroup)
 *
 * То такие случаи, мы будем парсить позже и результатом парсинга этого списка токенов[tokensRange] будет Какая-то реализация [ASTNode].
 * Когда мы попучит финальную версию этого куска дерева, а не этот placeholder, мы должны будем заменить в дереве этот Объект на результат парсинга.
 * Этот код должен быть в callback [doAfterParse]
 *
 * @param tokensRange список токенов для парсинга позже
 * @param expectedNodeType Тип ноды в которую парсить этот Объект.
 * @param doAfterParse действие заменяющие в дереве нас на наш результат парсинга
 * @param T Наследник от ASTNode
 */
class LateParseNode<T : ASTNode>(
    val tokensRange: TokensRange,
    val expectedNodeType: KClass<T>,
    val doAfterParse: (T) -> Unit
) : ASTNode {
    operator fun invoke(result: ASTNode) {
        if (expectedNodeType.isInstance(expectedNodeType)) this.doAfterParse.invoke(result as T)
        else throw RuntimeException("mismatch type of ASTNode expected=${expectedNodeType}, actual=${result}")
    }
}

class SelectNode : ExpressionNode, Alias {
    /** Keyword flag == DISTINCT */
    var isDistinct: Boolean = false

    /** Keyword flag == ALL */
    var isAll: Boolean = false

    /**
     * Cписок Колонок, а точнее того, что можно принять за Колонку
     *
     *      | Node type      | Text                         | Description                    |
     *      |--------------- |------------------------------|--------------------------------|
     *      | ColumnNode     | id                           | field                          |
     *      | ColumnNode     | users.id                     | table + field                  |
     *      | ColumnNode     | public.users.id              | schema + table + field         |
     *      | ColumnNode     | public.users.id AS ID        | schema + table + field + alias |
     *      | ExpressionNode | count(*)                     | func call                      |
     *      | ExpressionNode | count(*) * 10                | func call + expression         |
     *      | ExpressionNode | count(*) * 10 AS shift_count | func call + expression + alias |
     *      | SelectNode     | (select * from posts)        | sub-select                     |
     *      | SelectNode     | (select * from posts) AS p   | sub-select  + alias            |
     *
     * Каждый из таких вариантов валидный и поэтому, приведена таблица Реализаций [ASTNode] что реально могут тут оказаться.
     * * При написании транслятора, можно делать when (...) {...} с проверкой класса ноды
     */
    val columns: MutableList<ASTNode> = mutableListOf()
    override var alias: String = ""
}

class ColumnNode : ExpressionNode, Alias {
    var schema: String = ""
    var table: String = ""
    var field: String = ""
    override var alias: String = ""
}

class FunctionCallNode : ExpressionNode, Alias {
    var name: String = ""
    val args: MutableList<ExpressionNode> = mutableListOf()

    override var alias: String = ""
}

/**
 * Технический класс, представляет список [ExpressionNode] как цельный [ExpressionNode] + функционал (Deque | Коллекции).
 * Аналог SortedAndSelfBalancedBinaryExpressionTree, в котором root это нода с Самым-самый низким по
 * приоритету оператор и 2-мя (ValueNode | OperatorNode | FunctionNode) нодама-детьми.
 *
 * Я отказался от Парсинга в Дерево т.к.
 * 1 - это тяжело сделать с Сортировкой дерева по ходу парсинга БЕЗ Рекурсии - у меня не хватило мозгов
 * 2 - можно парсить в обычное бинарное дерево, но без сортировки, оно по сути бесполезно для Транслятора.
 * 3 - на стеке можно сделать не хуже, чем через дерево без сортировки.

 *
 * С точки зрения написания Транслятора: это можно считать одним оператором/действием,
 * выполним которое, мы получим value, — так же как и получив мы Константу или выполни вызов Функции или Select
 * Применение:
 * - выражение с операторами
 *
 * ```
 * operator priority:     2   1   4       3    6      5
 * original text    : (12 + 4 / 2 <= :age * 2 || :age >= 18)
 * operator priority:         2          1           4         3         6         5
 * debug text       : (12     +     4    /     2     <=  :age  *    2    ||  :age  >=    18)
 * stack content    : [const, op, const, op, const, bop, var, op, const, bop, var, bop, const]
 * ```
 * - скобки внутри expression - todo Уверен?
 * ```
 * operator priority:   2   3    1    4
 * original text    : 5 * 7 + (3 * 3) + 10
 * operator priority:          2          3                    1                4
 * debug text       : 5        *     7    +         (      3   *     3      )   +    10
 * stack content    : [const, op, const, op, stack[l_p, const, op, const, r_p], op, const]
 * ```
 *
 * - когда наш expression состоит больше чем 1 select или function call или константа
 * ```
 * operator priority:   1         2
 * original text    : SUM(salary) * 10
 * operator priority:    1               2
 * debug text       :  SUM(salary)  *    10
 * stack content    : [func[expr], op, const]
 * ```
 * * случаи когда у нас только 1 select или function call или константа, это можно сразу парсить в:
 * [SelectNode] || [FunctionCallNode] || [ConstantNode].
 *
 * - todo - что делать с Рекурсивным моментом?
 */
class ExpressionStack : ExpressionNode {
    val stack: LinkedList<ExpressionNode> = LinkedList()
}

class OperatorNode(val token: Token) : ExpressionNode

class ConstantNode(val token: Token) : ExpressionNode

fun main() {
    var root: SelectNode? = null
    val o = LateParseNode(
        TokensRange(
            listOf(),
            0
        ),
        SelectNode::class,
    ) {}
    val analyseTokens: List<Token> = LinkedList()
    ParserV3().parseAST(
        LateParseNode(TokensRange(analyseTokens, 0, analyseTokens.size), SelectNode::class) { root = it },
        LinkedList()
    )
}

class ParserV3 {
    /**
     * Ключевой идеи парсинга без рекурсии является:
     * Что мы будем делать когда/если столкнулись с Рекурсивным моментом? (Select в Select'е)
     *
     * В Данном случаи используется подход, когда:
     * Группируем все токены Рекурсивного момента в объект [LateParseNode].
     * Добавляем объект [LateParseNode] в дерево где должна находится настоявшая рекурсивная Нода.
     * Складываем в Стек/Очередь (lateParseNodsStack), что бы распарсить их позже.
     * Назначаем в callback [LateParseNode.doAfterParse] как и где, нам замети [LateParseNode] на настоявшую рекурсивную Ноду.
     */
    fun parseV3(analyseTokens: List<Token>): SelectNode {
        val lateParseNodsStack = LinkedList<LateParseNode<*>>()
        val rootParseNode = LateParseNode(TokensRange(analyseTokens, 0, analyseTokens.size), SelectNode::class) {}
        val root: SelectNode =
            parseAST(rootParseNode, lateParseNodsStack) as SelectNode // если тут упало - значит плохо написал parseSelect

        // если мы не нашли рекурсивных нод, то идём дальше на выход
        while (lateParseNodsStack.isNotEmpty()) {
            val curr: LateParseNode<*> = lateParseNodsStack.pollLast() // обход в глубину
            val currResult: ASTNode = parseAST(curr, lateParseNodsStack)
//        curr(currResult)
        }

        return root
    }

    fun <T : ASTNode> parseAST(lateParseNode: LateParseNode<T>, lateParseStack: Deque<LateParseNode<*>>): ASTNode =
        when (lateParseNode.expectedNodeType) {
            in SelectNode::class -> parseSelect(lateParseNode, lateParseStack)
            in ExpressionStack::class -> parseExpressionStack(lateParseNode, lateParseStack)
            in ExpressionNode::class -> parseExpression(lateParseNode, lateParseStack)
            else -> lateParseNode // не повезло, не фортануло, нарвались на кейс когда данный тип выражений ПОКА-ЧТО не парситься глубже.
        }


    private fun <T : ASTNode> parseSelect(
        lateParseNode: LateParseNode<T>,
        lateParseStack: Deque<LateParseNode<*>>
    ): ASTNode {
        TODO("Not yet implemented")
    }

    private fun <T : ASTNode> parseExpression(
        lateParseNode: LateParseNode<T>,
        lateParseStack: Deque<LateParseNode<*>>
    ): ExpressionNode {
        TODO("Not yet implemented")
    }


    private fun <T : ASTNode> parseExpressionStack(
        lateParseNode: LateParseNode<T>,
        lateParseStack: Deque<LateParseNode<*>>
    ): ExpressionNode {
        TODO("Not yet implemented")
    }

    private infix fun KClass<*>.isInHierarchyOf(possibleUpperClass: KClass<*>): Boolean =
        possibleUpperClass::class.java.isAssignableFrom(this.java) || this == possibleUpperClass

    private operator fun KClass<*>.contains(possibleUpperClass: KClass<*>): Boolean =
        this isInHierarchyOf possibleUpperClass
}