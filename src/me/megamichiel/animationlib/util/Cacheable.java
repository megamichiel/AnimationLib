package me.megamichiel.animationlib.util;

import java.util.function.Function;
import java.util.function.Supplier;

public class Cacheable<T, C, O> {

    private final Function<T, C> mapper;
    private final Function<T, O> dynamicAction;
    private final Function<C, O> cachedAction;

    public Cacheable(Function<T, C> mapper,
                     Function<T, O> dynamicAction,
                     Function<C, O> cachedAction) {
        this.mapper = mapper;
        this.dynamicAction = dynamicAction;
        this.cachedAction = cachedAction;
    }

    public Supplier<O> get(T value) {
        C cached = mapper.apply(value);
        if (cached != null) return () -> cachedAction.apply(cached);
        return () -> dynamicAction.apply(value);
    }
}
