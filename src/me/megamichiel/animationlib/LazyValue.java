package me.megamichiel.animationlib;

import java.util.function.Supplier;

public class LazyValue<T> implements Supplier<T> {

    public static <T> LazyValue<T> of(Supplier<T> supplier) {
        return new LazyValue<>(supplier);
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
}
