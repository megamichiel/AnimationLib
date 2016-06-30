package me.megamichiel.animationlib.placeholder;

import me.megamichiel.animationlib.Nagger;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public final class PlaceholderContext {

    private final Map<Player, Map<String, Object>> values = new HashMap<>();

    private final Nagger nagger;

    private PlaceholderContext(Nagger nagger) {
        this.nagger = nagger;
    }

    public Object get(Player who, String identifier) {
        Map<String, Object> map = values.get(who);
        return map != null ? map.get(identifier) : null;
    }

    public void set(Player who, String identifier, Object value) {
        Map<String, Object> map = values.get(who);
        if (map == null) values.put(who, map = new HashMap<>());
        map.put(identifier, value);
    }

    public <T> T invoke(Player who, String identifier, IPlaceholder<T> placeholder) {
        Map<String, Object> map = values.get(who);
        T result = null;
        if (map == null) values.put(who, map = new HashMap<>());
        else if ((result = (T) map.get(identifier)) != null) return result;
        map.put(identifier, result = placeholder.invoke(nagger, who));
        return result;
    }

    public static PlaceholderContext create(Nagger nagger) {
        return new PlaceholderContext(nagger);
    }
}
