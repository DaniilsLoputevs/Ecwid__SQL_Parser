package v1

import io.kotest.core.spec.style.FunSpec
import v1.TokenType.*

/**
 * testcases:
 * + "SELECT * from users;" // canonical
 * + "SELECT 'text_text_value'" // text value
 * + "SELECT now()" // func value
 * + "SELECT 10" // number value
 * + "SELECT * from users" // no semicolon
 * + "SELECT id from users" // no semicolon + one field
 * - "SELECT id, name from users" // no semicolon + 2 fields field
 * - "SELECT users.id, users.name from users" // no semicolon + 2 fields field + table.field
 * - "SELECT u.id, u.name from users u" // no semicolon + 2 fields field + alias.field
 * - "SELECT COUNT(*) users;" // func + arg: star
 * - "SELECT COUNT(id) users;" // func + arg: column
 * - "SELECT CONCAT('Hello, ', name) users;" // func + arg: string value + column
 *
 *
 * + "SELECT    id,\r  name  \r\n  from \n users" // no semicolon + 2 fields field + space & tab chars
 *
 * Random examples:
 * - SELECT f.title, f.did, d.name, f.date_prod, f.kind
 *     FROM distributors d JOIN films f USING (did);
 * - SELECT kind, sum(len) AS total FROM films GROUP BY kind;
 * - SELECT kind, sum(len) AS total
 *     FROM films
 *     GROUP BY kind
 *     HAVING sum(len) < interval '5 hours';
 * - SELECT * FROM distributors ORDER BY name;
 * - SELECT * FROM distributors ORDER BY 2;
 * - SELECT m.name AS mname, pname
 * FROM manufacturers m, LATERAL get_product_names(m.id) pname;
 * - SELECT m.name AS mname, pname
 * FROM manufacturers m LEFT JOIN LATERAL get_product_names(m.id) pname ON true;
 * - SELECT 2+2;
 * - SELECT TOP 5 C.CustomerID,
 *          (SELECT SUM(TotalDue) FROM Sales.SalesOrderHeader H
 *           WHERE H.CustomerID = C.CustomerID) AS TotalDue
 *   FROM Sales.Customer C
 *   ORDER BY TotalDue DESC;
 * - SELECT * FROM (SELECT * FROM my_table FOR UPDATE) ss WHERE col1 = 5;
 * -
 * -
 * -
 * -
 */
class LexerTest : FunSpec({
    val test = Lexer()
    val debug = LexerDebug()

    test("canonical") {
        testByAllContainsOrdered(
            test, "SELECT * from users;",
            tokenStub(type = SELECT), tokenStub(type = SPACE),
            tokenStub(type = STAR), tokenStub(type = SPACE),
            tokenStub(type = FROM), tokenStub(type = SPACE),
            tokenStub(type = EXPRESSION),
            tokenStub(type = SEMICOLON),
        )
    }
    test("text value") {
        testByAllContainsOrdered(
            debug, "SELECT 'text_text_value'",
            tokenStub(type = SELECT), tokenStub(type = SPACE),
            tokenStub(type = EXPRESSION, text = "'text_text_value'"),
        )
    }
    test("number value") {
        testByAllContainsOrdered(
            debug, "SELECT 10",
            tokenStub(type = SELECT), tokenStub(type = SPACE),
            tokenStub(type = EXPRESSION, text = "10"),
        )
    }
    test("func value") {
        testByAllContainsOrdered(
            test, "SELECT now()",
            tokenStub(type = SELECT), tokenStub(type = SPACE),
            tokenStub(type = EXPRESSION), tokenStub(type = L_PAR), tokenStub(type = R_PAR),
        )
    }
    test("no semicolon") {
        testByAllContainsOrdered(
            test, "SELECT * from users",
            tokenStub(type = SELECT), tokenStub(type = SPACE),
            tokenStub(type = STAR), tokenStub(type = SPACE),
            tokenStub(type = FROM), tokenStub(type = SPACE),
            tokenStub(type = EXPRESSION),
        )
    }
    test("no semicolon + one field") {
        testByAllContainsOrdered(
            test, "SELECT id from users",
            tokenStub(type = SELECT), tokenStub(type = SPACE),
            tokenStub(type = EXPRESSION), tokenStub(type = SPACE),
            tokenStub(type = FROM), tokenStub(type = SPACE),
            tokenStub(type = EXPRESSION),
        )
    }
    test("keywords with Lower & Upper cases") {
        testByAllContainsOrdered(
            test, "SeLeCt id fRoM users",
            tokenStub(type = SELECT), tokenStub(type = SPACE),
            tokenStub(type = EXPRESSION), tokenStub(type = SPACE),
            tokenStub(type = FROM), tokenStub(type = SPACE),
            tokenStub(type = EXPRESSION),
        )
    }
    test("no semicolon + 2 fields field + space & tab chars") {
        testByAllContainsOrdered(
            debug, "SELECT    id,\r  name  \r\n  from \n users",
            tokenStub(type = SELECT), tokenStub(type = SPACE),
            tokenStub(type = EXPRESSION),tokenStub(type = COMMA), tokenStub(type = SPACE),
            tokenStub(type = EXPRESSION),tokenStub(type = SPACE),
            tokenStub(type = FROM), tokenStub(type = SPACE),
            tokenStub(type = EXPRESSION),
        )
    }


})