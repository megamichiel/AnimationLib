package me.megamichiel.animationlib.bukkit.placeholder;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CustomPlaceholder implements BiFunction<Player, String, String> {

    private final Map<String, Function<Player, String>> matches = new HashMap<>();
    private final Map<String, BiFunction<Player, String, String>> startsWith = new HashMap<>();
    private BiFunction<Player, String, String> lastResort;

    protected CustomPlaceholder(Plugin plugin, String identifier) {
        PapiPlaceholder.register(plugin, identifier, this);
        MVdWPlaceholder.register(plugin, identifier, this);
    }

    protected CustomPlaceholder matches(String text, Function<Player, String> function) {
        matches.put(text, function);
        return this;
    }

    protected CustomPlaceholder startsWith(String text, BiFunction<Player, String, String> function) {
        startsWith.put(text, function);
        return this;
    }

    protected CustomPlaceholder lastResort(BiFunction<Player, String, String> function) {
        this.lastResort = function;
        return this;
    }

    @Override
    public final String apply(Player player, String s) {
        Function<Player, String> function = matches.get(s);
        if (function != null) return function.apply(player);
        for (Map.Entry<String, BiFunction<Player, String, String>> entry : startsWith.entrySet()) {
            if (s.startsWith(entry.getKey())) {
                return entry.getValue().apply(player, s.substring(entry.getKey().length()));
            }
        }
        return lastResort != null ? lastResort.apply(player, s) : null;
    }
}
