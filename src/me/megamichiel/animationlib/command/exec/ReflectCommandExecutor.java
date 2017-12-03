package me.megamichiel.animationlib.command.exec;

import me.megamichiel.animationlib.command.BaseCommandAPI;
import me.megamichiel.animationlib.command.annotation.*;
import me.megamichiel.animationlib.command.annotation.Optional;
import me.megamichiel.animationlib.command.arg.CommandResultHandler;
import me.megamichiel.animationlib.command.arg.CustomArgument;
import me.megamichiel.animationlib.command.arg.DelegateArgument;
import me.megamichiel.animationlib.command.arg.Priority;
import me.megamichiel.animationlib.command.ex.CommandException;
import me.megamichiel.animationlib.command.ex.InvalidUsageException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class ReflectCommandExecutor implements CommandExecutor {

    private static final Map<Class<?>, Function<String, Object>> parsers = new HashMap<>();
    private static final Map<Class<?>, String> defaults = new HashMap<>();

    private static void putParser(Class<?> wrapper, Class<?> primitive,
                                  Function<String, Object> parser, String def) {
        parsers.put(wrapper, parser);
        parsers.put(primitive, parser);
        defaults.put(primitive, def);
    }

    static {
        putParser(Byte.class, Byte.TYPE, Byte::valueOf, "0");
        putParser(Short.class, Short.TYPE, Short::valueOf, "0");
        putParser(Integer.class, Integer.TYPE, Integer::valueOf, "0");
        putParser(Long.class, Long.TYPE, Long::valueOf, "0");
        putParser(Float.class, Float.TYPE, Float::valueOf, "0");
        putParser(Double.class, Double.TYPE, Double::valueOf, "0");
        putParser(Boolean.class, Boolean.TYPE, s -> {
            if (s.equalsIgnoreCase("true")) return Boolean.TRUE;
            if (s.equalsIgnoreCase("false")) return Boolean.FALSE;
            throw new NumberFormatException("Must be either true or false!");
        }, "false");
        putParser(Character.class, Character.TYPE, s -> {
            if (s.length() == 1) return s.charAt(0);
            throw new NumberFormatException("Must be 1 character long!");
        }, "\u0000");
    }

    private final BaseCommandAPI<?, ?> api;
    private final String red;
    private final CommandAdapter adapter;
    private final CommandHandler handler;

    private final ArgumentParser parser;
    private final List<ArgumentParser> subcommands = new ArrayList<>();
    private Method tabCompleter;

    public ReflectCommandExecutor(BaseCommandAPI<?, ?> api, String red,
                                  CommandAdapter adapter, Method method,
                                  CommandHandler handler)
            throws IllegalArgumentException {
        this.api = api;
        this.red = red;
        this.adapter = adapter;
        this.handler = handler;

        Class<?>[] params = method.getParameterTypes();

        if (params.length == 0)
            throw new IllegalArgumentException("Must have at least 1 parameter!");
        Annotation[][] annotations = method.getParameterAnnotations();
        Alias alias;
        boolean hasInfo = params.length > 1
                && (alias = getAnnotation(annotations[1], Alias.class)) != null
                && alias.value().equals("@info");
        parser = new ArgumentParser(method, params, annotations, hasInfo);
    }

    public CommandHandler getHandler() {
        return handler;
    }

    @Override
    public void onCommand(CommandContext ctx) {
        Object sender = ctx.getSender();
        Object cmd = ctx.getCommand();
        String label = ctx.getLabel();
        String[] args = ctx.getArgs();
        for (ArgumentParser sub : subcommands) {
            try {
                parse(sub, sender, cmd, label, args);
                return;
            } catch (InvalidUsageException ex) {
                // Possibly incorrect subcommand
            } catch (IllegalArgumentException ex) {
                api.sendMessage(sender, red + ex.getMessage());
                return;
            }
        }
        try {
            parse(parser, sender, cmd, label, args);
        } catch (IllegalArgumentException ex) {
            api.sendMessage(sender, red + ex.getMessage());
        } catch (InvalidUsageException ex) {
            if (!handler.usage().isEmpty()) {
                api.sendMessage(sender, red + handler.usage().replace("<command>", label));
            } else {
                StringBuilder sb = new StringBuilder(red).append(parser.getUsage(sender, label));
                for (ArgumentParser subcommand : subcommands) {
                    sb.append(" OR ").append(subcommand.getUsage(sender, label));
                }
                api.sendMessage(sender, sb.toString());
            }
        }
    }

    @Override
    public void tabComplete(TabCompleteContext ctx) {
        if (tabCompleter == null) return;
        try {
            tabCompleter.invoke(adapter, ctx);
        } catch (Exception ex) {
            ctx.sendMessage(red + "An error occurred on performing this command");
            ex.printStackTrace();
        }
    }

    private void parse(ArgumentParser parser, Object sender, Object cmd, String label,
                       String[] args) throws IllegalArgumentException, InvalidUsageException {
        Object[] parsed = parser.parse(sender, cmd, label, args);
        if (parsed == null) return;
        try {
            Object o = parser.method.invoke(adapter, parsed);
            if (o != null) {
                if (o instanceof Boolean) {
                    if (!(Boolean) o) {
                        api.sendMessage(sender, red + parser.getUsage(sender, label));
                    }
                } else if (o instanceof String) {
                    api.sendMessage(sender, (String) o);
                } else {
                    CommandResultHandler handler = api.getCommandResultHandler(o.getClass());
                    if (handler != null) {
                        handler.onCommandResult(sender, o);
                    }
                }
            }
        } catch (Exception ex) {
            if (ex instanceof InvocationTargetException) {
                Throwable cause = ex.getCause();
                if (cause instanceof CommandException) {
                    throw cause instanceof InvalidUsageException ? (InvalidUsageException) cause : new IllegalArgumentException(cause.getMessage());
                }
            }
            api.sendMessage(sender, red + "An error occurred on performing this command");
            ex.printStackTrace();
        }
    }

    public void addSubcommand(Method method) {
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 1 && params[0] == TabCompleteContext.class) {
            if (tabCompleter == null) {
                method.setAccessible(true);
                tabCompleter = method;
            }
            return;
        }
        if (params.length == 0) {
            throw new IllegalArgumentException("Must have at least 1 parameter!");
        }
        Annotation[][] annotations = method.getParameterAnnotations();
        Alias alias;
        boolean hasInfo = params.length > 1
                && (alias = getAnnotation(annotations[1], Alias.class)) != null
                && alias.value().equals("@info");
        subcommands.add(new ArgumentParser(method, params, annotations, hasInfo));
    }

    private class ArgumentParser {

        private final Method method;
        private final Class<?> sender;
        private final Argument[] arguments;
        private final int offset;
        private final boolean label;

        private ArgumentParser(Method method, Class<?>[] params,
                               Annotation[][] annotations, boolean hasInfo)
                throws IllegalArgumentException {
            this.method = method;
            method.setAccessible(true);
            this.sender = params[0];
            label = hasInfo && params[1] == String.class;
            offset = label ? 2 : 1;
            arguments = new Argument[params.length - offset];

            CommandArguments aliases = method.getAnnotation(CommandArguments.class);
            String[] names = aliases == null ? null : aliases.value();
            for (int i = offset, length = params.length; i < length; i++) {
                try {
                    arguments[i - offset] = new Argument(params[i], annotations[i],
                            names == null ? null : names[i - offset]);
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Failed to parse argument " + i + "!", ex);
                }
            }
        }

        Object[] parse(Object sender, Object cmd, String label, String[] args)
                throws IllegalArgumentException {
            if (this.sender.isInstance(sender)) {
                Object[] res = new Object[arguments.length + offset];

                res[0] = sender;
                if (offset == 2) res[1] = this.label ? label : cmd;

                new ArgumentIterator(arguments, args, res, offset).parse(sender);

                return res;
            }
            return null;
        }

        String getUsage(Object sender, String label) {
            if (!handler.usage().isEmpty())
                return handler.usage().replace("<command>", label);
            StringBuilder sb = new StringBuilder('/' + label);
            boolean optional;
            for (Argument arg : arguments) {
                sb.append((optional = arg.isOptional(sender)) ? " [" : " <")
                        .append(arg.alias).append(optional ? ']' : '>');
            }
            return sb.toString();
        }
    }

    private class ArgumentIterator {

        private final Argument[] arguments;
        private final String[] args;
        private final Object[] values;
        private final boolean[] set;
        private final int offset;

        private int index = 0, parseIndex = 0;

        private IllegalArgumentException thrown, array;

        private ArgumentIterator(Argument[] arguments, String[] args,
                                 Object[] values, int offset) {
            this.arguments = arguments;
            this.args = args;
            this.values = values;
            set = new boolean[arguments.length];
            this.offset = offset;
        }

        void parse(Object sender) throws IllegalArgumentException {
            while (index < arguments.length) {
                Argument arg = arguments[index];
                if (set[index]) continue;
                if (parseIndex == args.length) {
                    if (arg.array != null && (arg.arrayInfo == null || arg.arrayInfo.min() <= 0)) {
                        List<Object> list = new ArrayList<>();
                        if (arg.arrayInfo != null) for (String s : arg.arrayInfo.def()) {
                            try {
                                list.add(arg.parse(sender, s));
                            } catch (IllegalArgumentException ex) {
                                System.err.println("[AnimationLib] Invalid default arg for " + arg.alias + ": '" + s + '\'');
                            }
                        }
                        put(index++, list.toArray((Object[]) Array.newInstance(arg.array, list.size())));
                        continue;
                    } else if (arg.isOptional(sender)) {
                        if (arg.defaultValue != null)
                            put(index, arg.parse(sender, arg.defaultValue));
                        index++;
                        continue;
                    } else if (thrown != null) throw thrown;
                    else throw new InvalidUsageException();
                }
                getValue(sender, arg);
            }
            if (parseIndex < args.length) {
                if (array == null) throw new InvalidUsageException();
                throw new IllegalArgumentException(array.getMessage());
            }
        }

        void getValue(Object sender, Argument arg) throws IllegalArgumentException {
            if (arg.array != null) {
                List<Object> list = new ArrayList<>();
                int i = index;
                for (;;) {
                    try {
                        Object o = arg.parse(sender, args[parseIndex]);
                        if (findNext(sender, arg.priority)) break;
                        list.add(o);
                        if (++parseIndex == args.length) break;
                    } catch (IllegalArgumentException ex) {
                        if (i == arguments.length - 1) array = ex;
                        break;
                    }
                }
                ArrayInfo info = arg.arrayInfo;
                if (info != null) {
                    int size = list.size();
                    if (size < info.min())
                        throw new IllegalArgumentException("Please specify at least " + info.min() + ' ' + arg.alias);
                    if (info.max() != 0 && size > info.max())
                        throw new IllegalArgumentException("Please specify at most " + info.max() + ' ' + arg.alias);
                    else if (size == 0 && info.def().length != 0) {
                        for (String s : info.def()) try {
                            list.add(arg.parse(sender, s));
                        } catch (IllegalArgumentException ex) {
                            System.err.println("[AnimationLib] Invalid default arg for " + arg.alias + ": '" + s + '\'');
                        }
                    }
                }
                if (i == index) index++;
                put(i, list.toArray((Object[]) Array.newInstance(arg.array, list.size())));
            } else {
                try {
                    Object o = arg.parse(sender, args[parseIndex]);
                    if (!arg.isOptional(sender) || !findNext(sender, arg.priority)) {
                        parseIndex++;
                        put(index, o);
                    }
                } catch (IllegalArgumentException ex) {
                    if (!arg.isOptional(sender))
                        throw new IllegalArgumentException("Invalid " + arg.alias + ": " + ex.getMessage());
                    if (thrown == null) thrown = ex;
                    if (arg.defaultValue != null)
                        put(index, arg.parse(sender, arg.defaultValue));
                }
                index++;
            }
        }

        boolean findNext(Object sender, Priority priority) {
            int current = index + 1;
            int i = priority.ordinal();
            Argument arg;
            while (current != arguments.length) {
                arg = arguments[current];
                int old = index;
                if (arg.priority.ordinal() > i) try {
                    index = current;
                    getValue(sender, arg);
                    return true;
                } catch (IllegalArgumentException ex) {
                    index = old;
                    current++; // Continue
                }
                if (!arg.isOptional(sender)) break;
            }
            return false;
        }

        void put(int index, Object value) {
            values[index + offset] = value;
            set[index] = true;
        }
    }

    private class Argument {

        private final Class<?> array;
        private final ArrayInfo arrayInfo;
        private final Predicate<Object> optional;
        private final String alias;
        private final Priority priority;

        private final BiFunction<Object, String, Object> parser;
        private final String defaultValue;

        private Argument(Class<?> type, Annotation[] annotations, String name) {
            arrayInfo = getAnnotation(annotations, ArrayInfo.class);
            Optional optional = getAnnotation(annotations, Optional.class);
            if (type.isArray()) {
                array = type = type.getComponentType();
                if (type.isArray())
                    throw new IllegalArgumentException("No multidimensional arrays allowed!");
                this.optional = o -> false;
            } else {
                array = null;
                if (optional != null) {
                    Class<?>[] senders = optional.asSender();
                    switch (senders.length) {
                        case 0: this.optional = o -> true; break;
                        case 1: this.optional = senders[0]::isInstance; break;
                        default: this.optional = o -> {
                            for (Class<?> c : senders)
                                if (c.isInstance(o)) return true;
                            return false;
                        }; break;
                    }
                } else this.optional = o -> false;
            }
            Alias alias = getAnnotation(annotations, Alias.class);
            if (alias != null) this.alias = alias.value();
            else if (name != null) this.alias = name;
            else this.alias = type.getSimpleName().toLowerCase(Locale.ENGLISH);
            if (type == String.class) {
                parser = (sender, s) -> s;
                priority = Priority.LOW;
            } else if (type.isEnum()) {
                Class<? extends Enum> e = type.asSubclass(Enum.class);
                parser = (sender, s) -> Enum.valueOf(e, s.toUpperCase(Locale.ENGLISH).replace('-', '_'));
                priority = Priority.HIGH;
            } else {
                Function<String, Object> func = parsers.get(type);
                if (func != null) {
                    parser = (sender, s) -> {
                        try {
                            return func.apply(s);
                        } catch (NumberFormatException ex) {
                            throw new IllegalArgumentException(ex.getMessage());
                        }
                    };
                    priority = Priority.HIGH;
                } else if (CustomArgument.class.isAssignableFrom(type)) {
                    Constructor<?> c = type.getConstructors()[0];
                    c.setAccessible(true);
                    parser = (sender, s) -> {
                        try {
                            return c.newInstance(sender, s);
                        } catch (IllegalArgumentException ex) {
                            throw ex;
                        } catch (Exception ex) {
                            if (ex instanceof InvocationTargetException
                                    && ex.getCause() instanceof IllegalArgumentException)
                                throw (IllegalArgumentException) ex.getCause();
                            throw new IllegalArgumentException(ex);
                        }
                    };
                    Priority p;
                    try {
                        Method m = type.getDeclaredMethod("getPriority");
                        m.setAccessible(true);
                        p = (Priority) m.invoke(null);
                    } catch (Exception ex) {
                        p = Priority.NORMAL;
                    }
                    priority = p;
                } else {
                    DelegateArgument<?, ?> delegate = api.getDelegateArgument(type);
                    if (delegate != null) {
                        parser = getParser(delegate);
                        priority = delegate.getPriority();
                    } else throw new IllegalArgumentException("No parser found for " + type + "!");
                }
            }
            String def = optional != null && !"@null"
                    .equals(optional.value()) ? optional.value() : null;
            defaultValue = def != null ? def : defaults.get(type);
        }

        private <S> BiFunction<Object, String, Object> getParser(DelegateArgument<S, ?> delegate) {
            return (sender, s) -> delegate.parse((S) sender, s);
        }

        Object parse(Object sender, String arg) throws IllegalArgumentException {
            return parser.apply(sender, arg);
        }

        boolean isOptional(Object sender) {
            return optional.test(sender);
        }

        @Override
        public String toString() {
            return "Argument[array=" + array + ", alias=" + alias
                    + ", priority=" + priority + ", def=" + defaultValue + ']';
        }
    }

    private static <A extends Annotation> A getAnnotation(
            Annotation[] annotations, Class<A> type) {
        for (Annotation a : annotations)
            if (type.isInstance(a)) return type.cast(a);
        return null;
    }
}
