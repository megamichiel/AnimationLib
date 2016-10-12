package me.megamichiel.animationlib;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class LazyValue<T> implements Supplier<T> {

    public static <T> LazyValue<T> of(Supplier<T> supplier) {
        return new LazyValue<>(supplier);
    }

    public static <T> LazyValue<T> unsafe(Callable<T> callable) {
        return new LazyValue<>(() -> {
            try {
                return callable.call();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private final Supplier<T> supplier;
    private T value;

    public LazyValue(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        return value == null ? (value = supplier.get()) : value;
    }

    public void reset() {
        value = null;
    }

    public void set(T t) {
        value = t;
    }
}
