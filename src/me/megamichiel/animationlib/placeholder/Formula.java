package me.megamichiel.animationlib.placeholder;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.ctx.ParsingContext;
import me.megamichiel.animationlib.util.StringReader;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

import static java.lang.Math.log;

public class Formula implements CtxPlaceholder<String> {

    private static DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);

    public static void setLocale(Locale locale) {
        Formula.symbols = new DecimalFormatSymbols(locale);
    }

    public static DecimalFormatSymbols getSymbols() {
        return symbols;
    }

    private static double getValue(Nagger nagger, Object who, Object o, PlaceholderContext ctx) {
        if (o instanceof Double) {
            return (Double) o;
        }
        if (o instanceof Formula) {
            return ((Formula) o).compute(nagger, who, ctx);
        }
        if (o instanceof IPlaceholder) {
            Object val = ((IPlaceholder) o).invoke(nagger, who, ctx);
            if (val instanceof Number) {
                return ((Number) val).doubleValue();
            }
            try {
                return val == null ? 0 : Double.parseDouble(val.toString());
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
        if (o instanceof UnaryFunction) {
            return ((UnaryFunction) o).compute(nagger, who, ctx);
        }
        throw new IllegalArgumentException("Unknown object: " + o);
    }

    private static UnaryOperator op(String id, DoubleUnaryOperator op) {
        return new UnaryOperator(id, op);
    }

    private static final UnaryOperator[] UNARY_OPERATORS = {
            new UnaryOperator("-", num -> -num, false),
            op("sqrt", Math::sqrt),
            op("square", num -> num * num),
            op("round", Math::round),
            op("floor", Math::floor),
            op("ceil", Math::ceil),
            op("ln", Math::log),
            op("sin", Math::sin), op("asin", Math::asin),
            op("cos", Math::cos), op("acos", Math::acos),
            op("tan", Math::tan), op("atan", Math::atan),
            op("abs", Math::abs),
            op("random", num -> Math.random() * num)
    };

    private static int P = 0;

    private static final Operator MULTIPLY, SUBTRACT;

    private static final Operator[] OPERATORS = {
            new Operator(P, "max", Math::max),
            new Operator(P, "min", Math::min),

            new Operator(++P, "^", Math::pow),
            new Operator(P, "log", (left, right) -> log(right) / log(left)),

            // Multiplication
            MULTIPLY = new Operator(++P, "*", (left, right) -> left * right),
            new Operator(P, "/", (left, right) -> left / right),
            new Operator(P, "mod", Math::IEEEremainder),

            // Addition
            new Operator(++P, "+", (left, right) -> left + right),
            SUBTRACT = new Operator(P, "-", (left, right) -> left - right)
    };

    public static Formula parse(String str, ParsingContext ctx) {
        return parse(new StringReader(str), ctx, false);
    }

    public static Formula parse(StringReader reader, ParsingContext ctx, boolean subgroup) {
        List<Object> values = new ArrayList<>();
        Operator last = null;
        String src = reader.source();
        boolean next = true;
        while (next) {
            reader.skipWhitespace();
            if (!reader.isReadable()) break;
            char c = reader.readChar();
            switch (c) {
                case '%':
                    if (!reader.isReadable())
                        throw new IllegalArgumentException(src + " ends with a placeholder character! Did you forget to escape it?");
                    StringBuilder sb = new StringBuilder();
                    while ((c = reader.readChar()) != '%') {
                        sb.append(c);
                        if (!reader.isReadable())
                            throw new IllegalArgumentException("Placeholder not closed off in " + src + "!");
                    }
                    values.add(StringBundle.createPlaceholder(sb.toString()));
                    break;
                case '(':
                    values.add(parse(reader, ctx, true));
                    break;
                case ')':
                    if (!subgroup) {
                        throw new IllegalArgumentException("Unexpected ) in " + src + " at index " + reader.index());
                    }
                    next = false;
                    break;
                case '{':
                    if (ctx == null) {
                        throw new IllegalArgumentException("Unknown character: " + c + " in "
                                + reader.source() + " at index " + reader.index());
                    }
                    if (!reader.isReadable()) {
                        throw new IllegalArgumentException(src + " ends with a special placeholder character! Did you forget to escape it?");
                    }
                    for (sb = new StringBuilder(); (c = reader.readChar()) != '}'; sb.append(c)) {
                        if (!reader.isReadable()) {
                            throw new IllegalArgumentException("Special placeholder not closed off in " + src + "!");
                        }
                    }
                    IPlaceholder<?> parsed;
                    String placeholder = sb.toString();
                    for (ParsingContext context = ctx; (parsed = context.parse(placeholder)) == null; ) {
                        if ((context = context.parent()) == null) {
                            throw new IllegalArgumentException("Unknown special placeholder: " + placeholder + '!');
                        }
                    }
                    values.add(parsed);
                    break;
                default:
                    if ((c >= '0' && c <= '9') || c == '.') { // Numba
                        sb = new StringBuilder();
                        do {
                            sb.append(c);
                            if (!reader.isReadable()) break;
                        } while (((c = reader.readChar()) >= '0' && c <= '9') || c == '.');

                        if ((c < '0' || c > '9') && c != '.') {
                            reader.unreadChar(); // Unread terminating non-digit
                        }
                        values.add(Double.valueOf(sb.toString()));
                    } else {
                        Operator operator = null;
                        for (Operator op : OPERATORS) {
                            if (op.id[0] == c) {
                                if (op.id.length > 1) {
                                    if (reader.isReadable() && reader.readChar() != op.id[1]) {
                                        reader.unreadChar();
                                        continue;
                                    }
                                    if (reader.isReadable() && op.id.length == 3 && reader.readChar() != op.id[2]) {
                                        reader.unread(2);
                                        continue;
                                    }
                                }
                                operator = op;
                                break;
                            }
                        }
                        if (operator != null) {
                            values.add(last == null || operator.level > last.level ? (last = operator) : operator);
                            break;
                        }
                        UnaryOperator unary = null;
                        int len;
                        reader.unreadChar(); // Unread to read char array properly
                        for (UnaryOperator op : UNARY_OPERATORS) {
                            if (op.id[0] == c) {
                                if ((len = op.id.length) != 1) {
                                    if (reader.available() + 1 < len)
                                        continue;
                                    if (!Arrays.equals(reader.readChars(len), op.id)) {
                                        reader.unread(len);
                                        continue;
                                    }
                                }
                                unary = op;
                                break;
                            }
                        }
                        if (unary != null) {
                            values.add(unary);
                            if (unary.requiresGroup) {
                                if (!(reader.isReadable() && reader.readChar() == '('))
                                    throw new IllegalArgumentException("Expected group after "
                                            + String.valueOf(unary.id) + " in " + reader.source());
                                values.add(parse(reader, ctx, true));
                            }
                            break;
                        } else {
                            reader.readChar(); // Re-read the starting char
                        }
                        throw new IllegalArgumentException("Unknown character: " + c + " in "
                                + reader.source() + " at index " + reader.index());
                    }
                    break;
            }
        }
        Object o = getElement(reader.source(), values, last, ctx);
        return o instanceof Formula ? (Formula) o : new Formula(null, ctx == null ? null : ctx.numberFormat(), o);
    }

    private static Object getElement(String src, List<Object> list,
                                     Operator op, ParsingContext ctx) {
        int size = list.size();
        if (size == 0) {
            throw new IllegalArgumentException("Empty group in " + src + "!");
        }
        Object first = list.get(0);
        if (size == 1) {
            if (first instanceof Operator || first instanceof UnaryOperator) {
                throw new IllegalArgumentException(src + " only consists of an operator!");
            }
            return first;
        }
        if (first instanceof Operator) {
            if (first == SUBTRACT) {
                list.set(0, UNARY_OPERATORS[0]); // Negate
            } else {
                throw new IllegalArgumentException(src + " starts with an operator!");
            }
        }
        if (list.get(size - 1) instanceof Operator) {
            throw new IllegalArgumentException(src + " ends with an operator!");
        }

        Object left, right;
        for (int i = size; i > 1; ) {
            right = list.get(--i);
            if ((left = list.get(i - 1)) instanceof Operator) {
                if (right instanceof Operator) {
                    if (right == SUBTRACT) {
                        list.set(i, UNARY_OPERATORS[0]); // Negate
                        continue;
                    }
                    throw new IllegalArgumentException("Double operators in " + src + "!");
                }
            } else if (left instanceof UnaryOperator) {
                if (right instanceof Operator) {
                    throw new IllegalArgumentException("Operator after unary operator in " + src + "!");
                }
                list.set(i, new UnaryFunction((UnaryOperator) left, right));
                list.remove(i - 1);
            } else if (!(right instanceof Operator || right instanceof Double)) {
                list.add(i, MULTIPLY); // Add implicit multiplication
            }
        }
        List<Object> sub = new ArrayList<>(), children = new ArrayList<>();
        for (Object o : list) {
            if (o == op) {
                Operator last = null;
                for (Object o2 : sub) {
                    if (o2 instanceof Operator && (last == null || ((Operator) o2).level > last.level)) {
                        last = (Operator) o2;
                    }
                }
                children.add(getElement(src, sub, last, ctx));
                sub.clear();
            } else {
                sub.add(o);
            }
        }
        if (!sub.isEmpty()) {
            Operator last = null;
            for (Object o : sub) {
                if (o instanceof Operator && (last == null || ((Operator) o).level > last.level)) {
                    last = (Operator) o;
                }
            }
            children.add(getElement(src, sub, last, ctx));
        }
        return new Formula(op, ctx == null ? null : ctx.numberFormat(), children.toArray());
    }

    private final Operator operator;
    private final NumberFormat format;
    private final Object[] values;

    public Formula(Operator operator, NumberFormat format, Object... values) {
        this.operator = operator;
        this.format = format;
        this.values = values;
    }

    public double compute(Nagger nagger, Object who, PlaceholderContext ctx) {
        int length = values.length;
        if (length == 0) {
            return 0;
        }
        double out = getValue(nagger, who, values[0], ctx);
        for (int i = 1; i < length; ) {
            out = operator.computer.applyAsDouble(out, getValue(nagger, who, values[i++], ctx));
        }
        return out;
    }

    @Override
    public String invoke(Nagger nagger, Object who, PlaceholderContext ctx) {
        double d = compute(nagger, who, ctx);
        return format == null ? Double.toString(d) : format.format(d);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(").append(values[0]);
        for (int i = 1; i < values.length; ++i) {
            sb.append(operator.id).append(values[i]);
        }
        return sb.append(')').toString();
    }

    private static class Operator {

        private final int level;
        private final char[] id;
        private final DoubleBinaryOperator computer;

        private Operator(int lvl, String id, DoubleBinaryOperator computer) {
            level = lvl;
            this.id = id.toCharArray();
            this.computer = computer;
        }

        @Override
        public String toString() {
            return String.valueOf(id);
        }
    }

    private static class UnaryOperator {

        private final char[] id;
        private final DoubleUnaryOperator computer;
        private final boolean requiresGroup;

        private UnaryOperator(String id, DoubleUnaryOperator computer) {
            this(id, computer, true);
        }

        private UnaryOperator(String id, DoubleUnaryOperator computer,
                              boolean requiresGroup) {
            this.id = id.toCharArray();
            this.computer = computer;
            this.requiresGroup = requiresGroup;
        }

        @Override
        public String toString() {
            return String.valueOf(id);
        }
    }

    private static class UnaryFunction {

        private final UnaryOperator operator;
        private final Object value;

        private UnaryFunction(UnaryOperator operator, Object value) {
            this.operator = operator;
            this.value = value;
        }

        double compute(Nagger nagger, Object who, PlaceholderContext ctx) {
            return operator.computer.applyAsDouble(getValue(nagger, who, value, ctx));
        }

        @Override
        public String toString() {
            return operator.toString() + '(' + value + ')';
        }
    }
}
