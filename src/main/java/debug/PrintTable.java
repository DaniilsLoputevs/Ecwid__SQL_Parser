package debug;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static debug.CellColour.*;

/**

 * <p>
 * Class for (formatting || print || log || debug) Collection<Object> in pretty table human-readable view.
 * Implements the Builder pattern.
 * Compile on Java 9 and java 8(manually replace var -> actual variable type).
 * Excepted using is copy/paste this file into your project and use.
 *
 * @param <E> - Table element type - actual type of elements that store in table content
 * @author Daniils Loputevs
 * @version 4.0 - 18.04.2023
 * @see Column
 * @see ColumnOptions
 * @see DefaultFormatterBuilder
 * @see CellColour
 * @see AlignSide
 */
public class PrintTable<E> {
    private String name;
    private Collection<E> content;
    private final List<Column<E, ?>> columns = new ArrayList<>();
    
    
    /* STATIC */
    
    
    private static final String LS = System.lineSeparator();
    
    
    public static <E> PrintTable<E> of(Collection<E> content) {
        var rsl = new PrintTable<E>();
        rsl.content = new ArrayList<>(content);
        return rsl;
    }
    
    /**
     * @implNote This operation will terminate input stream. {@param content}
     * Collect to new Mutable List in case, you can use {@link PrintTable#append(Object dataRow)}
     * and fall with exception: modification operation under ImmutableCollection.
     */
    public static <E> PrintTable<E> of(Stream<E> content) {
        var temp = new ArrayList<E>();
        content.forEach(temp::add);
        return of(temp);
    }
    
    /**
     * Exists for use in functional style programming.
     * Example:
     * <pre>{@code
     *      PrintTable<String> table = PrintTable.<String>ofAppendDataLater()
     *         .indexColumn()
     *         .column("VALUE", it -> it)
     *         .column("SIZE", it -> it);
     *      List<String> stringList = Stream.of("a", "bb", "ccc")
     *         .map(it -> it)        // imitate some business logic
     *         .filter(__ -> true)   // imitate some business logic
     *         .peek(table::append)
     *         .toList();            // imitate some business logic
     *      table.print();
     * }</pre>
     */
    public static <E> PrintTable<E> ofAppendDataLater() {
        return of(new ArrayList<>());
    }
    
    
    /* OBJECT METHODS */
    
    
    /** Set table name */
    public PrintTable<E> name(String tableName) {
        this.name = tableName;
        return this;
    }
    
    /** @see PrintTable#columnElemIndex(String name) */
    public PrintTable<E> columnElemIndex() {
        return this.columnElemIndex("#");
    }
    
    /**
     * Add column that show Index(order number) of elements in this table.
     *
     * @param name default value: "#"
     */
    public PrintTable<E> columnElemIndex(String name) {
        var counter = new int[]{0};
        return this.column(name, (__) -> counter[0]++, null, __ -> {});
    }
    
    
    /** @see PrintTable#column(String, Function, Function, Consumer) */
    public <CVT> PrintTable<E> column(String name, Function<E, CVT> getValue) {
        return this.column(name, getValue, null, __ -> {});
    }
    
    /** @see PrintTable#column(String, Function, Function, Consumer) */
    public <CVT> PrintTable<E> column(String name, Function<E, CVT> getValue, Function<CVT, String> formatter) {
        return this.column(name, getValue, formatter, __ -> {});
    }
    
    
    /** @see PrintTable#column(String, Function, Function, Consumer) */
    public <CVT> PrintTable<E> column(String name, Function<E, CVT> getValue, Consumer<ColumnOptions<E>> optionsConfig) {
        return this.column(name, getValue, null, optionsConfig);
    }
    
    /**
     * Add new column.
     * <p>
     * Use note:
     * {@param formatter} <br>
     * Expected use case:
     * <pre>
     * - display (Date || Time || DateTime) in custom format.
     * - locally override {@code Object.toString} for cell value.
     * - (collections || array) reduce/joining toString.
     * </pre>
     *
     * @param name          value for header.
     * @param getValue      getter - how to receive value for column cell from each row/data element.
     * @param formatter     (optional argument) cell value formatter/toStringMapper, applied for cell value.
     * @param optionsConfig (optional argument) extra configuration for column.
     * @param <CVT>         Cell value type - it's different for each column.
     * @return this for chain
     */
    public <CVT> PrintTable<E> column(String name, Function<E, CVT> getValue,
                                      Function<CVT, String> formatter,
                                      Consumer<ColumnOptions<E>> optionsConfig
    ) {
        var options = new ColumnOptions<E>();
        optionsConfig.accept(options);
        columns.add(new Column<>(name, getValue, formatter, options));
        return this;
    }
    
    /**
     * Exists for use in functional style programming.
     * Example:
     * <pre>{@code
     *      PrintTable<String> table = PrintTable.<String>ofAppendDataLater()
     *         .indexColumn()
     *         .column("VALUE", it -> it)
     *         .column("SIZE", it -> it);
     *      List<String> stringList = Stream.of("a", "bb", "ccc")
     *         .map(it -> it)        // imitate some business logic
     *         .filter(__ -> true)   // imitate some business logic
     *         .peek(table::append)
     *         .toList();            // imitate some business logic
     *      table.print();
     * }</pre>
     *
     * @param dataRow add data to table inner content.
     */
    public E append(E dataRow) {
        content.add(dataRow);
        return dataRow;
    }
    
    
    @Override
    public String toString() {
        return new StringJoiner(System.lineSeparator())
                .add((name == null) ? "" : name + " (table size: " + content.size() + ')')
                .add(this.buildTable())
                .toString();
    }
    
    
    /* TERMINAL OPERATIONS */
    
    
    public void print() {
        System.out.println(this);
    }
    
    public void toAppendable(Appendable appendable) {
        try {
            appendable.append(this.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void toConsumer(Consumer<String> consumer) {
        consumer.accept(this.toString());
    }
    
    
    /* PRIVATE PART */
    
    
    private String buildTable() {
        var rsl = new StringBuilder();
        
        columns.forEach(it -> it.prepareAllCellsContent(content));
        
        var header = buildHeader();
        var splitLine = "+" + repeatChar('-', header.length() - 2) + "+";
        
        rsl.append(splitLine).append(LS);
        rsl.append(header).append(LS);
        rsl.append(splitLine).append(LS);
        
        for (int i = 0; i < content.size(); i++)
            rsl.append(buildLine(i)).append(LS);
        rsl.append(splitLine).append(LS);
        
        return rsl.toString();
    }
    
    private String buildHeader() {
        var rsl = new StringBuilder();
        for (var column : columns) {
            rsl.append("| ");
            
            // todo = var cellContentLength = column.name.length()
            var minWidth = Optional.ofNullable(column.options).map(ColumnOptions::getMinWidth).orElse(ColumnOptions.AUTO_FIT);
            var alignSpaceNum = (ColumnOptions.AUTO_FIT.equals(minWidth))
                    ? column.maxCellWidth - column.name.length()
                    : Integer.parseInt(minWidth) - column.name.length();
            
            var cellContent = column.name;
            var alignSide = Optional.ofNullable(column.options).map(ColumnOptions::getAlignSide).orElse(AlignSide.LEFT);
            if (alignSpaceNum > 0) {
                if (alignSide == AlignSide.LEFT)
                    rsl.append(cellContent).append(repeatChar(' ', alignSpaceNum));
                else if (alignSide == AlignSide.RIGHT)
                    rsl.append(repeatChar(' ', alignSpaceNum - 1)).append(cellContent);
            } else rsl.append(cellContent);
            rsl.append(' ');
        }
        rsl.append('|');
        return rsl.toString();
    }
    
    private String buildLine(int lineIndex) {
        var rsl = new StringBuilder();
        for (var column : columns) {
            var cellContent = column.content[lineIndex];
            rsl.append("| ");
            
            var minWidth = Optional.ofNullable(column.options).map(ColumnOptions::getMinWidth).orElse(ColumnOptions.AUTO_FIT);
            var cellContentLength = (column.isCellColorized[lineIndex])
                    ? cellContent.length() - Colour.COLOUR_STRING_EXTRA_LENGTH
                    : cellContent.length();
            var alignSpaceNum = (ColumnOptions.AUTO_FIT.equals(minWidth))
                    ? column.maxCellWidth - cellContentLength
                    : Integer.parseInt(minWidth) - cellContentLength;
            
            var alignSide = Optional.ofNullable(column.options).map(ColumnOptions::getAlignSide).orElse(AlignSide.LEFT);
            if (alignSpaceNum > 0) {
                if (alignSide == AlignSide.LEFT)
                    rsl.append(cellContent).append(repeatChar(' ', alignSpaceNum));
                else if (alignSide == AlignSide.RIGHT)
                    rsl.append(repeatChar(' ', alignSpaceNum - 1)).append(cellContent);
            } else rsl.append(cellContent);
            rsl.append(' ');
        }
        rsl.append('|');
        return rsl.toString();
    }
    
    private String repeatChar(char c, int times) {
        return String.valueOf(c).repeat(Math.max(0, times));
    }
    
    
    /* SUPPORT CLASSES */
   
}


class Column<E, CVT> {
    /* all final fields - init in constructor */
    final String name;
    final Function<E, CVT> getValue;
    final Function<CVT, String> formatter;
    final ColumnOptions<E> options;
    
    String[] content;
    boolean[] isCellColorized;
    int maxCellWidth = 0;
    
    public Column(String name, Function<E, CVT> getValue, Function<CVT, String> formatter, ColumnOptions<E> options) {
        this.name = name;
        this.getValue = getValue;
        this.formatter = formatter;
        this.options = options;
    }
    
    public void prepareAllCellsContent(Collection<E> tableContent) {
        this.content = new String[tableContent.size()];
        this.isCellColorized = new boolean[tableContent.size()];
        
        int contentIndex = 0;
        for (var each : tableContent) {
            var cellValue = cellValueToString(getValue.apply(each), formatter);
            var cellValueWidth = cellValue.length();
            
            if (options != null) {
                for (var cc : options.getCellColours()) {
                    if (cc.getCheck().test(each)) {
                        cellValue = Colour.colourise(cellValue, cc.getColour());
                        isCellColorized[contentIndex] = true;
                    }
                }
            }
            
            content[contentIndex] = cellValue;
            if (cellValueWidth > maxCellWidth) maxCellWidth = cellValueWidth;
            contentIndex++;
        }
        maxCellWidth = Math.max(maxCellWidth, name.length());
    }
    
    private String cellValueToString(CVT maybeArray, Function<CVT, String> cellValueFormatter) {
        if (cellValueFormatter != null) return cellValueFormatter.apply(maybeArray);
        if (maybeArray instanceof Object[]) return Arrays.toString((Object[]) maybeArray);
        if (maybeArray instanceof boolean[]) return Arrays.toString((boolean[]) maybeArray);
        if (maybeArray instanceof byte[]) return Arrays.toString((byte[]) maybeArray);
        if (maybeArray instanceof short[]) return Arrays.toString((short[]) maybeArray);
        if (maybeArray instanceof char[]) return Arrays.toString((char[]) maybeArray);
        if (maybeArray instanceof int[]) return Arrays.toString((int[]) maybeArray);
        if (maybeArray instanceof long[]) return Arrays.toString((long[]) maybeArray);
        if (maybeArray instanceof float[]) return Arrays.toString((float[]) maybeArray);
        if (maybeArray instanceof double[]) return Arrays.toString((double[]) maybeArray);
        return String.valueOf(maybeArray); // avoid NPE
    }
}

// todo - doc

/**
 * Shortcut for <pre>{@code new ColumnOptions(...);}</pre>
 * Example: <pre>{@code
 *   todo - impl example
 *  }</>
 */
@SuppressWarnings("unchecked")
  class ColumnOptions<E> {
    private AlignSide alignSide;
    private String minWidth;
    private final List<CellColour<E>> cellColours = new ArrayList<>();
    
    
    /**
     * default column width value.
     * column width = max string length of(cell value after formatter apply).
     */
    public static final String AUTO_FIT = "%AUTO_FIT%";
    
    
    // todo - doc
    public ColumnOptions<E> align(AlignSide side) {
        this.alignSide = side;
        return  this;
    }
    
    // todo - doc
    
    /**
     * Set param (min column width)
     * default || value not set: {@link ColumnOptions#AUTO_FIT}
     */
    public ColumnOptions<E> minWidth(int width) {
        this.minWidth = Integer.toString(width);
        return this;
    }
    
    @SuppressWarnings("rawtypes")
    public ColumnOptions<E> cellColours(Colour colour, Predicate<E> check) {
        this.cellColours.add(new CellColour(colour, check));
        return  this;
    }
    
    // todo - doc
    public ColumnOptions<E> cellColours(Collection<CellColour<E>> cellColours) {
        this.cellColours.addAll(cellColours);
        return this;
    }
    
    
    public AlignSide getAlignSide() {
        return alignSide;
    }
    
    public String getMinWidth() {
        return minWidth;
    }
    
    public List<CellColour<E>> getCellColours() {
        return cellColours;
    }
    
}
 class DefaultFormatterBuilder {
    /** @see DefaultFormatterBuilder#decimal(String) */
    public static Function<? super Number, String> decimal() {
        return decimal("####.0000");
    }
    
    /**
     * <a href="https://www.baeldung.com/java-decimalformat">more about Java Decimal Format</a>
     *
     * @see DecimalFormat DecimalFormat class doc for full list of acceptable symbols.
     */
    public static Function<? super Number, String> decimal(String pattern) {
        return numberObj -> (numberObj == null)
                ? String.valueOf((Object) null)
                : new DecimalFormat(pattern).format(numberObj);
    }
    
    /** @see DefaultFormatterBuilder#localDateTime(String) */
    public static Function<LocalDateTime, String> localDateTime() {
        return localDateTime("yyyy-MM-dd HH:mm:ss");
    }
    
    /**
     * @param pattern {@link DateTimeFormatter} check class doc to a full list of symbols.
     */
    public static Function<LocalDateTime, String> localDateTime(String pattern) {
        return numberObj -> (numberObj == null)
                ? String.valueOf((Object) null)
                : DateTimeFormatter.ofPattern(pattern).format(numberObj);
    }
    
    public static Function<Iterable<?>, String> iterableSize() {
        return iterable -> "" + sizeOfIterable(iterable);
    }
    
    private static <E> int sizeOfIterable(Iterable<E> iterable) {
        if (iterable instanceof Collection<E>) return ((Collection<E>) iterable).size();
        else if (iterable == null) return 0;
        else {
            final Integer[] size = {0};
            iterable.forEach(__ -> size[0]++);
            return size[0];
        }
    }
}

// todo - doc
 class CellColour<E> {
    private final Colour colour;
    private final Predicate<E> check;
    
    public CellColour(Colour colour, Predicate<E> check) {
        this.colour = colour;
        this.check = check;
    }
    
    public Colour getColour() {
        return colour;
    }
    
    public Predicate<E> getCheck() {
        return check;
    }
    
    /* Colours for text */
    public static final String
            TEXT_RESET = "\u001B[0m",
            TEXT_BLACK = "\u001B[30m",
            TEXT_RED = "\u001B[31m",
            TEXT_GREEN = "\u001B[32m",
            TEXT_YELLOW = "\u001B[33m",
            TEXT_BLUE = "\u001B[34m",
            TEXT_PURPLE = "\u001B[35m",
            TEXT_CYAN = "\u001B[36m",
            TEXT_WHITE = "\u001B[37m",
            BACKGROUND_BLACK = "\u001B[40m",
            BACKGROUND_RED = "\u001B[41m",
            BACKGROUND_GREEN = "\u001B[42m",
            BACKGROUND_YELLOW = "\u001B[43m",
            BACKGROUND_BLUE = "\u001B[44m",
            BACKGROUND_MAGENTA = "\u001B[45m",
            BACKGROUND_CYAN = "\u001B[46m",
            BACKGROUND_WHITE = "\u001B[47m";
}

 enum AlignSide {LEFT, RIGHT}

/**
 * colourise string -> always add 9 char for {@code String.length()}.
 */
 enum Colour {
    BLACK(TEXT_BLACK),
    RED(TEXT_RED),
    GREEN(TEXT_GREEN),
    YELLOW(TEXT_YELLOW),
    BLUE(TEXT_BLUE),
    PURPLE(TEXT_PURPLE),
    CYAN(TEXT_CYAN),
    WHITE(TEXT_WHITE),
    DEFAULT(TEXT_RESET);
    
    
    private final String txtColour;
    
    Colour(String txtColour) {
        this.txtColour = txtColour;
    }
    
    public static final int COLOUR_STRING_EXTRA_LENGTH = 9;
    
    public static String colourise(Object txt, Colour colour) {
        return colour.txtColour + txt + TEXT_RESET;
    }
}
