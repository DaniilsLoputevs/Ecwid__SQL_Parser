package v1

import debug.PrintTable
import io.kotest.core.test.TestScope
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import java.util.*

class LexerDebug : Lexer() {
    override fun analyse(sql: String): LinkedList<Token> {
        println("Debug print!")
        println("\"$sql\"")
        return super.analyse(sql).also { it.printLexAnalyse(sql) }
    }
}
fun tokenStub(
    startIndex: Int = -1,
    endIndex: Int = -1,
    text: String = "",
    type: TokenType
) = Token(startIndex, endIndex, text, type)

fun TestScope.testByAllContainsOrdered(
    lexer : Lexer,
    sql : String,
    vararg expected: Token,
) {
    val actual = lexer.analyse(sql)

    val actualIter = actual.iterator()
    val expectedIter = expected.iterator()

    actual shouldHaveSize (expected.size)

    while (actualIter.hasNext()) {
        val token = actualIter.next()
        val expect = expectedIter.next()
        token should (TypeMatcher(expect))
        if (expect.text.isNotEmpty()) token should (TextMatcher(expect))
        if (expect.startIndex != -1) token should (StartIndexMatcher(expect))
        if (expect.endIndex != -1) token should (EndIndexMatcher(expect))
    }
}

class TypeMatcher(private val token: Token) : Matcher<Token> {
    override fun test(value: Token) = MatcherResult.Companion.invoke(
        value.type == token.type,
        { "mismatch Token.type" },
        { "mismatch Token.type" }
    )
}

class TextMatcher(private val token: Token) : Matcher<Token> {
    override fun test(value: Token): MatcherResult {
        return MatcherResult.Companion.invoke(
            value.text == token.text,
            { "mismatch Token.text" },
            { "mismatch Token.text" })
    }
}

class StartIndexMatcher(private val token: Token) : Matcher<Token> {
    override fun test(value: Token): MatcherResult {
        return MatcherResult.Companion.invoke(
            value.startIndex == token.startIndex,
            { "mismatch Token.startIndex" },
            { "mismatch Token.startIndex" })
    }
}

class EndIndexMatcher(private val token: Token) : Matcher<Token> {
    override fun test(value: Token): MatcherResult {
        return MatcherResult.Companion.invoke(
            value.endIndex == token.endIndex,
            { "mismatch Token.endIndex" },
            { "mismatch Token.endIndex" })
    }
}


fun Collection<Token>.printLexAnalyse(originalSql : String) : Collection<Token> = this.also {
    PrintTable.of(this)
        .columnElemIndex()
        .column("TYPE", Token::type)
        .column("START", Token::startIndex)
        .column("END", Token::endIndex)
        .column("S-CHAR") { originalSql[it.startIndex] }
        .column("E-CHAR") { originalSql[it.endIndex] }
        .column("TEXT", Token::text)
        .print()
}
