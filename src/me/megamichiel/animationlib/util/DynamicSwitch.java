package me.megamichiel.animationlib.util;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class DynamicSwitch<T> implements Function<T, Object> {

    @SafeVarargs
    public static <T> DynamicSwitch<T> of(ReturningExecutor def, Case<? extends T>... cases) {
        return new DynamicSwitch<>(def, cases);
    }

    @SafeVarargs
    public static <T> DynamicSwitch<T> of(Case<? extends T>... cases) {
        return new DynamicSwitch<>(null, cases);
    }

    private final ReturningExecutor def;
    private final Case<? extends T>[] cases;

    @SafeVarargs
    private DynamicSwitch(ReturningExecutor def, Case<? extends T>... cases) {
        this.def = def == null ? () -> null : def;
        this.cases = cases;
    }

    @Override
    public Object apply(T value) {
        return apply(value, null);
    }

    public Object apply(T value, Consumer<? super Exception> exceptionHandler) {
        try {
            for (int i = 0; i < cases.length; i++) {
                if (cases[i].test(value)) {
                    for (; i < cases.length; i++) {
                        Result result = cases[i].executor.call();
                        if (result == Result.BREAK) return null;
                        if (result != Result.FALL_THROUGH) return result.value;
                    }
                    return def.execute();
                }
            }
            return def.execute();
        } catch (Exception thrown) {
            if (exceptionHandler != null) exceptionHandler.accept(thrown);
            return null;
        }
    }

    @SafeVarargs
    public static <T> Case<T> Case(T value, RunnableExecutor executor, T... extra) {
        return new Case<>(value, () -> {
            executor.execute();
            return Result.BREAK;
        }, extra);
    }

    @SafeVarargs
    public static <T> Case<T> Case(T value, PredicateExecutor executor, T... extra) {
        return new Case<>(value, () -> executor.execute() ? Result.FALL_THROUGH : Result.BREAK, extra);
    }

    @SafeVarargs
    public static <T> Case<T> Case(T value, boolean result, T... extra) {
        return new Case<>(value, () -> result ? Result.FALL_THROUGH : Result.BREAK, extra);
    }

    @SafeVarargs
    public static <T> Case<T> Case(T value, ReturningExecutor executor, T... extra) {
        return new Case<>(value, () -> new Result(executor.execute()), extra);
    }

    @SafeVarargs
    public static <T> Case<T> Case(T value, Object result, T... extra) {
        return new Case<>(value, () -> new Result(result), extra);
    }

    public static class Case<T> {

        private final T value;
        private final Callable<Result> executor;
        private final T[] extra;

        private Case(T value, Callable<Result> executor, T[] extra) {
            this.value = value;
            this.executor = executor;
            this.extra = extra;
        }

        private boolean test(Object value) {
            if (value == null) {
                if (this.value == null) return true;
                for (T t : extra) if (t == null) return true;
            } else if (value.equals(this.value)) return true;
            else for (T t : extra) if (value.equals(t)) return true;
            return false;
        }
    }

    public static class Result {

        public static final Result BREAK = new Result(null),
                                   FALL_THROUGH = new Result(null);

        private final Object value;

        private Result(Object value) {
            this.value = value;
        }
    }

    public interface ReturningExecutor {
        Object execute() throws Exception;
    }

    public interface RunnableExecutor {
        void execute() throws Exception;
    }

    public interface PredicateExecutor {
        boolean execute() throws Exception;
    }
}
