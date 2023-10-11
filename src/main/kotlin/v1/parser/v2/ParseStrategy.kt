package v1.parser.v2

import v1.Token
import v1.TokenType
import v1.TokenType.*

sealed class ParseStrategy {
    abstract fun accept(tokenIndex: Int, token: Token, type: TokenType): ParseAnswer
    abstract fun doOnFinish()
    abstract fun reset(offsetTokenIndex: Int)

    class SELECT(
        val qContainer: ParserV2.QContainer,
        val analyseTokens: List<Token>,
        offsetTokenIndex: Int
    ) : ParseStrategy() {
        private var tokensRange = TokensRange(analyseTokens, offsetTokenIndex)
        private var tokenLvl = 0

        override fun accept(tokenIndex: Int, token: Token, type: TokenType): ParseAnswer {
            return when (type) {
                COMMA -> {
                    if (tokenLvl > 0) {
                        tokensRange.count++
                        ParseAnswer.CONTINUE
                    } else{
                        this.doOnFinish()
                        this.reset(tokenIndex)
                        ParseAnswer.NEXT
                    }
                }
                FROM  -> {
                    this.doOnFinish()
                    ParseAnswer.SWITCH
                }
                L_PAR -> {
                    tokensRange.count++
                    tokenLvl++
                    ParseAnswer.CONTINUE
                }
                R_PAR -> {
                    tokensRange.count++
                    tokenLvl--
                    ParseAnswer.CONTINUE
                }
                else -> {
                    tokensRange.count++
                    ParseAnswer.CONTINUE
                }
            }
        }

        override fun doOnFinish() {
            qContainer.columns += tokensRange
        }

        override fun reset(offsetTokenIndex: Int) {
            tokensRange = TokensRange(analyseTokens, offsetTokenIndex)
        }

    }

    class FROM(
        val qContainer: ParserV2.QContainer,
        val analyseTokens: List<Token>,
        offsetTokenIndex: Int
    ) : ParseStrategy() {
        private var tokensRange = TokensRange(analyseTokens, offsetTokenIndex)
        private var tokenLvl = 0

        override fun accept(tokenIndex: Int, token: Token, type: TokenType): ParseAnswer {
            // todo - подумать на счёт sub-select
            //          ? expectedParseType = ExpressionNode(interface) + when by pattern?
            return when (type) {
                COMMA -> {
                    if (tokenLvl > 0) {
                        tokensRange.count++
                        ParseAnswer.CONTINUE
                    } else{
                        this.doOnFinish()
                        this.reset(tokenIndex)
                        ParseAnswer.NEXT
                    }
                }
                JOIN, WHERE, GROUP, HAVING , LIMIT, OFFSET -> { // todo join это внтрянка для From

                    this.doOnFinish()
                    ParseAnswer.SWITCH
                }
                L_PAR -> {
                    tokensRange.count++
                    tokenLvl++
                    ParseAnswer.CONTINUE
                }
                R_PAR -> {
                    tokensRange.count++
                    tokenLvl--
                    ParseAnswer.CONTINUE
                }
                else -> {
                    tokensRange.count++
                    ParseAnswer.CONTINUE
                }
            }
        }

        override fun doOnFinish() {
            qContainer.tables += tokensRange
        }

        override fun reset(offsetTokenIndex: Int) {
            tokensRange = TokensRange(analyseTokens, offsetTokenIndex)
        }

    }

    abstract class JOIN() : ParseStrategy()
    data object STUB : ParseStrategy() {
        override fun accept(tokenIndex: Int, token: Token, type: TokenType): ParseAnswer {
            TODO("Not yet implemented")
        }

        override fun doOnFinish() {
            TODO("Not yet implemented")
        }

        override fun reset(offsetTokenIndex: Int) {
            TODO("Not yet implemented")
        }
    }
//    companion object {
//        fun of(tokenType: TokenType) = when (tokenType) {
//            TokenType.FROM ->
//        }
//    }

    enum class ParseAnswer {
        CONTINUE, // Всё ОК, продолжаем кушать токены
        NEXT, // Встретили Запятую - закончили с одной частью, вызови build() + reset() и парсим след. часть
        SWITCH, // Встретили KEYWORD что означает, что наша часть кончилась и нужно парсить другую часть,
        FINISH,
    }

}