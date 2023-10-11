# Ecwid__SQL_Parser

Этот Репозиторий, попытка решить [тестовое задание от компании Ecwid](https://github.com/Ecwid/new-job/blob/master/SQL-parser.md)

## Так же, это проба выполнить Challenges:
- Писать решение с прицелом, что в будущем можно развить до полноценного Компилятора для языка запросов SQL с минимальным набором элементов
- - Lexer - входной SQL String разбить на список Токенов
- - Parser - входной список Токенов распарсить в AST (Abstract Syntax Tree) 
- - Translator - входное AST транслировать в команды(код) для манимуляции данными
- - - Translator - тут не будет, я к такому не готов
- Парсить не только Самый-Популярный синтаксис, но и не столь распространённый.
- обойтись без RegExp ВЕЗДЕ, где это реально и хватает мозгов.
- обойтись без Рекурсии ВЕЗДЕ, где это реально и хватает мозгов.
- по возможности сделать этот код быстрым и простым + отработка темы Паттерны проектирования

## Технологии:

- Kotlin 1.9.0
- Kotest 5.7.2 (test framework)

## Tests

для запуска тестов рекомендуется установить [IDEA plugin Kotest](https://plugins.jetbrains.com/plugin/14080-kotest)

## Дополнительные материалы, собранные по ходу разработки:

- [Схема Select syntax (postgres)](https://www.postgresql.org/docs/current/sql-select.html)
- [Список postgres built-in functions](https://cloud.google.com/spanner/docs/reference/postgresql/functions-and-operators)
- 
- [YouTube: Тестовое задание Java Kotlin - SQL Parser](https://www.youtube.com/watch?v=XD72j6o9zIA)
- [YouTube: SQL Parser Решения кандидатов](https://www.youtube.com/watch?v=VRWreSsSt7c)
- [YouTube: SQL Parser, Infix, Extensions, Kotlin, DSL](https://www.youtube.com/watch?v=ggg-IFOheig)
- [YouTube: Создаем свой ЯП. Лексер, Парсер, Абстрактное синтаксическое дерево (AST)](https://www.youtube.com/watch?v=Ezt3vBok5_s)
-
- [пример решения #1](https://github.com/k1ll1n/ecwid-sql-parser/tree/master/src/main/kotlin/net/example/sqlp)
- [пример решения #2](https://github.com/anasasiia/sql-parser)


## Что не поддерживается 
- Валидация входящего SQL - мы заранее насчитываем что к нам приходит валидный SQL.
- JSON - в стандарте SQL есть синтаксис работы с JSON - такие штуки для меня слишком сложно парсить.
- Arrays - ? - пока не понятно как парсить такие значения, пока этот синтаксис не планируется поддерживать. 
