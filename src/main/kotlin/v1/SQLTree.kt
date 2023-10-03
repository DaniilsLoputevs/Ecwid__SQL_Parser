package v1

import java.util.*

class SQLTree {

}

/**
 * TableExpression
 * ColumnExpression
 * ValueExpression
 * PredicateExpression {
 *  -
 *  }
 *
 */
/**
 * ColumnExpression
 * - (function || combination of functions),
 * -- todo ? value [1,2, 'aaa', true]
 * -- todo https://cloud.google.com/spanner/docs/reference/postgresql/functions-and-operators
 * - schema + table + column,
 * - SELECT (валидно, только если SELECT вернёт таблицу с Одной колонкой ИЛИ после expression есть УКАЗАНИЕ на колонку...)
 *      - вынести в отдельную tableExpression
 * - (* - star)
 */
interface ColumnExpression {
    //    fun lexStack(): Queue<String>
    val lexStack: Queue<String>
    val name: String
    val alias: String
}

class SchemaTableColumn(
    val schema: String = "",
    val table: String = "",
    val column: String,
    override val alias: String = ""
) : ColumnExpression {
    override val lexStack: Queue<String>
        get() = TODO("Not yet implemented")
    override val name: String
        get() = "$schema${if (schema != "") "." else ""}" +
                "$table${if (table != "") "." else ""}" +
                column + if (alias != "") " AS $alias" else ""
//    override val alias: String
//        get() = TODO("Not yet implemented")
}

object Star : ColumnExpression {
    override val lexStack: Queue<String>
        get() = LinkedList<String>().apply { this += "+" }
    override val name: String
        get() = "*"
    override val alias: String
        get() = "*"

}

fun main() {
    println()
    val input = "SELECT author.name, count(book.id), sum(book.cost) \n" +
            "FROM author \n" +
            "LEFT JOIN book ON (author.id = book.author_id) \n" +
            "GROUP BY author.name \n" +
            "HAVING COUNT(*) > 1 AND SUM(book.cost) > 500\n" +
            "LIMIT 10;"

    val columnStar: ColumnExpression = Star.apply { println(name) }
    val column0: ColumnExpression = SchemaTableColumn(column = "id").apply { println(this.name) }
    val column1: ColumnExpression = SchemaTableColumn(table = "users", column = "id").apply { println(this.name) }
    val column2: ColumnExpression = SchemaTableColumn("public", "users", "id").apply { println(this.name) }
    val column3: ColumnExpression = SchemaTableColumn("public", "users", "id", "id").apply { println(this.name) }
}
//internal class Query {
//    private val columns: List<String>? = null
//    private val fromSources: List<Source>? = null
//    private val joins: List<Join>? = null
//    private val whereClauses: List<WhereClause>? = null
//    private val groupByColumns: List<String>? = null
//    private val sortColumns: List<Sort>? = null
//    private val limit: Int? = null
//    private val offset: Int? = null
//}
/*
SQL Examples:

SELECT author.name, count(book.id), sum(book.cost)
FROM author
LEFT JOIN book ON (author.id = book.author_id)
GROUP BY author.name
HAVING COUNT(*) > 1 AND SUM(book.cost) > 500
LIMIT 10;

SELECT
    employees.employee_id,
    employees.first_name,
    employees.last_name,
    departments.department_name,
    (SELECT MAX(salary_amount) FROM salaries WHERE employee_id = employees.employee_id) AS max_salary,
    (SELECT COUNT(*) FROM orders WHERE employee_id = employees.employee_id) AS order_count
FROM employees
JOIN departments ON employees.department_id = departments.department_id;

SELECT employees.employee_id, employees.first_name, employees.last_name, departments.department_name, salaries.salary_amount
FROM employees
JOIN departments ON employees.department_id = departments.department_id
JOIN (
    SELECT employee_id, MAX(salary_amount) AS salary_amount
    FROM salaries
    GROUP BY employee_id
) AS salaries ON employees.employee_id = salaries.employee_id;

 */