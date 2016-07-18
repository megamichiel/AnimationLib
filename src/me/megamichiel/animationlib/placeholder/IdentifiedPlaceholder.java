package me.megamichiel.animationlib.placeholder;

import me.megamichiel.animationlib.Nagger;

public class IdentifiedPlaceholder<T> implements IPlaceholder<T> {

    private final String identifier;
    private final IPlaceholder<T> placeholder;

    public IdentifiedPlaceholder(String identifier, IPlaceholder<T> placeholder) {
        this.identifier = identifier;
        this.placeholder = placeholder;
    }

    public String getIdentifier() {
        return identifier;
    }

    public IPlaceholder<T> getPlaceholder() {
        return placeholder;
    }

    @Override
    public T invoke(Nagger nagger, Object who) {
        return placeholder.invoke(nagger, who);
    }
}
