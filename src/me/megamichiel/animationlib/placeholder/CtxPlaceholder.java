package me.megamichiel.animationlib.placeholder;

import me.megamichiel.animationlib.Nagger;

public interface CtxPlaceholder<T> extends IPlaceholder<T> {

    static <T> CtxPlaceholder<T> constant(T value) {
        return (n, p, c) -> value;
    }

    @Override
    default T invoke(Nagger nagger, Object who) {
        return invoke(nagger, who, null);
    }

    @Override
    T invoke(Nagger nagger, Object who, PlaceholderContext ctx);
}
