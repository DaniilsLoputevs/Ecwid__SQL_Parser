package v1.parser.v2

import v1.Token

class TokensRange(
    val analyseTokens: List<Token>,
    val offsetIndex: Int,
) : AbstractList<Token>() {
    var count: Int = 0
    override val size: Int = count

    /**
     * offsetIndex: 5
     * count: 6
     * analyseTokens: [ SELECT id , users.name , SUBSTR ( 'abc' ,  2   )   FROM users , attrs ]
     * tokenIndexes:  0   1    2  3     4      5  6     7   8   9  10  11   12   13  14  15
     * this.toString(): "SUBSTR ( 'abc' , 2 )"
     * innerTokenIndex:    0    1   2   3 4 5
     *
     */
    override fun get(innerTokenIndex: Int): Token {
        if (innerTokenIndex < 0 || innerTokenIndex > count - 1)
            throw IndexOutOfBoundsException("innerTokenIndex($innerTokenIndex) is out of range[0-${count - 1}]")
        return analyseTokens[innerTokenIndex - offsetIndex]
    }


    override operator fun iterator() = TokenIterator()

    override fun toString(): String = this.joinToString(" ") { token -> token.text }

    inner class TokenIterator : ListIterator<Token> {
        val firstTokenIndex: Int get() = offsetIndex + 1
        val lastTokenIndex: Int get() = offsetIndex + count
        private var index: Int = firstTokenIndex

        override fun hasNext(): Boolean = index <= lastTokenIndex
        override fun hasPrevious(): Boolean = index >= lastTokenIndex
        override fun next(): Token = analyseTokens[index++]
        override fun previous(): Token = analyseTokens[index--]
        override fun nextIndex(): Int = index + 1
        override fun previousIndex(): Int = index - 1

        override fun toString(): String {
            val sb = StringBuilder()
            for (i in firstTokenIndex..lastTokenIndex) {
                sb.append(analyseTokens[i].text)
                if (i != lastTokenIndex) sb.append(" ")
            }
            return sb.toString()
        }
    }
}
