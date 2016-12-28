package me.megamichiel.animationlib.placeholder;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.ctx.SimplePlaceholderContext;

public interface PlaceholderContext {

    Object get(Object who, String identifier);

    @SuppressWarnings("unchecked")
    <T> T invoke(Object who, String identifier, IPlaceholder<T> placeholder);

    static PlaceholderContext create(Nagger nagger) {
        return new SimplePlaceholderContext(nagger);
    }
}
