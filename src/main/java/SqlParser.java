//import lombok.Builder;
//
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
///**
// * Убирать из входной строки:
// * - перенос строк "/t"
// * - больше одного пробелы (https://www.baeldung.com/java-regex-s-splus)
// * Query.Paring(offset, limit)
// */
//public class SqlParser {
//    public static void main(String[] args) {
//        var in1 = "SELECT * FROM items WHERE id = 1";
//
//    }
//
//    public static Object parse(String src) {
//        var stack = new LinkedList<Lex>();
//
//        for (var token : src.split(" ")) {
//            switch (token) {
//                case "SELECT", "select" -> {
//                }
//                default -> {
//
//                }
//            }
//        }
//        return null;
//    }
//
//    interface Lex {
//        default String text() {return "";};
//    }
//
//    @Builder
//    static class SelectLex implements Lex {
//        List<String> columns = new ArrayList<>();
//        String from;
//
//    }
//
//    @Builder
//    static class WhereLex implements Lex {
//
//    }
//}
