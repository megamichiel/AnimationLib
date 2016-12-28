package me.megamichiel.animationlib.placeholder.ctx;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.PlaceholderContext;

import java.util.HashMap;
import java.util.Map;

public class SimplePlaceholderContext implements PlaceholderContext {

    // {Player::{Identifier::Value}}
    private final Map<Object, Map<String, Object>> values = new HashMap<>();

    private final Nagger nagger;

    public SimplePlaceholderContext(Nagger nagger) {
        this.nagger = nagger;
    }

    @Override
    public Object get(Object who, String identifier) {
        Map<String, Object> map = values.get(who);
        return map != null ? map.get(identifier) : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T invoke(Object who, String identifier, IPlaceholder<T> placeholder) {
        return (T) values.computeIfAbsent(who, k -> new HashMap<>())
                .computeIfAbsent(identifier, k -> placeholder.invoke(nagger, who));
    }
}
