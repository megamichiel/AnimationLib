package me.megamichiel.animationlib.placeholder;

import me.megamichiel.animationlib.Nagger;

public interface CtxPlaceholder<T> extends IPlaceholder<T> {

    @Override
    default T invoke(Nagger nagger, Object who) {
        return invoke(nagger, who, null);
    }

    @Override
    T invoke(Nagger nagger, Object who, PlaceholderContext ctx);
}
