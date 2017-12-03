package me.megamichiel.animationlib.command;

import me.megamichiel.animationlib.command.annotation.Alias;
import me.megamichiel.animationlib.command.annotation.CommandHandler;
import me.megamichiel.animationlib.command.arg.CommandResultHandler;
import me.megamichiel.animationlib.command.arg.DelegateArgument;
import me.megamichiel.animationlib.command.exec.CommandAdapter;
import me.megamichiel.animationlib.command.exec.CommandContext;
import me.megamichiel.animationlib.command.exec.ReflectCommandExecutor;
import me.megamichiel.animationlib.util.Subscription;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class BaseCommandAPI<S, C> implements CommandAPI<S, C> {

    private static BaseCommandAPI instance;

    public static <S, C> BaseCommandAPI<S, C> getInstance() {
        return instance;
    }

    private final Map<Class<?>, DelegateArgument<S, ?>> delegateArguments = new HashMap<>();
    private final Map<Class<?>, CommandResultHandler<S, ?>> resultHandlers = new HashMap<>();

    protected final String red;

    protected BaseCommandAPI(String red) {
        if (instance == null) {
            instance = this;
        }
        this.red = red;
    }

    public abstract void sendMessage(Object sender, String message);

    @Override
    public List<CommandSubscription<C>> registerCommands(String plugin, CommandAdapter adapter) {
        if (plugin == null) {
            throw new NullPointerException("plugin");
        }
        if (adapter == null) {
            throw new NullPointerException("adapter");
        }
        List<ReflectCommandExecutor> list = new ArrayList<>();
        CommandHandler handler;
        Class<?> type = adapter.getClass();
        Method[] methods = type.getDeclaredMethods();
        for (Method method : methods) {
            if ((handler = method.getAnnotation(CommandHandler.class)) != null) {
                String[] names = handler.value();
                try {
                    if (names.length == 0)
                        throw new IllegalArgumentException("Method " + type.getName() + "::" + method.getName() + " has no names specified!");
                    list.add(new ReflectCommandExecutor(this, red, adapter, method, handler));
                } catch (IllegalArgumentException ex) {
                    System.err.println("[CommandAPI] Failed to register event for " + type.getName() + "::" + method.getName());
                    ex.printStackTrace();
                }
            }
        }
        Alias subcommand;
        ReflectCommandExecutor exec;
        for (Method method : methods) {
            if ((subcommand = method.getAnnotation(Alias.class)) != null) {
                String parent = subcommand.value();
                exec = list.stream().filter(e -> e.getHandler()
                        .value()[0].equalsIgnoreCase(parent))
                        .findAny().orElse(null);
                if (exec != null) exec.addSubcommand(method);
            }
        }
        return list.stream()
                .map(e -> registerCommand(plugin, new CommandInfo(e.getHandler(), e)))
                .collect(Collectors.toList());
    }

    @Override
    public List<CommandSubscription<C>> deleteCommands(String... names) {
        List<String> list = Arrays.asList(names);
        Set<C> deleted = new HashSet<>();
        deleteCommands((n, c) -> {
            if (list.contains(n)) {
                deleted.add(c);
                return true;
            }
            return false;
        });
        return deleteCommands((n, c) -> deleted.contains(c));
    }

    @Override
    public CommandSubscription<C> addCommandFilter(String command,
                                                Predicate<? super CommandContext> predicate,
                                                boolean tabComplete) {
        C cmd = getCommand(command);
        if (cmd == null) return null;
        List<CommandSubscription<C>> list = addCommandFilter(
                (s, c) -> s == cmd, predicate, tabComplete);
        return new CommandSubscription<C>(this, cmd) {
            @Override
            public void unsubscribe() {
                list.forEach(Subscription::unsubscribe);
                unsubscribed = true;
            }
        };
    }

    @Override
    public <T> Subscription registerDelegateArgument(Class<? super T> type, DelegateArgument<S, ? extends T> delegate) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        if (delegate == null) {
            throw new NullPointerException("delegate");
        }
        delegateArguments.put(type, delegate);
        return new Subscription() {
            @Override
            public void unsubscribe() {
                if (!isUnsubscribed())
                    delegateArguments.remove(type);
            }

            @Override
            public boolean isUnsubscribed() {
                return delegateArguments.get(type) != delegate;
            }
        };
    }

    public DelegateArgument getDelegateArgument(Class type) {
        return delegateArguments.get(type);
    }

    public <T> CommandResultHandler getCommandResultHandler(Class<T> type) {
        CommandResultHandler res = resultHandlers.get(type);
        if (res != null) return res;
        Class<? super T> parent = type.getSuperclass();
        if (parent != null) return getCommandResultHandler(parent);
        for (Class<?> c : type.getInterfaces())
            if ((res = getCommandResultHandler(c)) != null)
                return res;
        return null;
    }

    @Override
    public <T> Subscription registerCommandResultHandler(Class<? extends T> clazz, CommandResultHandler<S, ? super T> handler) {
        if (clazz == null) {
            throw new NullPointerException("clazz");
        }
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        resultHandlers.put(clazz, handler);
        return new Subscription() {
            @Override
            public void unsubscribe() {
                if (!isUnsubscribed()) resultHandlers.remove(clazz);
            }

            @Override
            public boolean isUnsubscribed() {
                return resultHandlers.get(clazz) != handler;
            }
        };
    }
}
