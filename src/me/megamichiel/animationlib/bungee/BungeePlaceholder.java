package me.megamichiel.animationlib.bungee;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.bungee.category.PlaceholderCategory;
import me.megamichiel.animationlib.bungee.category.PlayerCategory;
import me.megamichiel.animationlib.bungee.category.ServerCategory;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BungeePlaceholder implements IPlaceholder<String> {

    private final RegisteredPlaceholder placeholder;

    public BungeePlaceholder(String identifier) {
        this.placeholder = placeholders.get(identifier);
    }

    public BungeePlaceholder(RegisteredPlaceholder placeholder) {
        this.placeholder = placeholder;
    }

    @Override
    public String invoke(Nagger nagger, Object who) {
        return placeholder.invoke(nagger, (ProxiedPlayer) who);
    }

    private static final List<PlaceholderCategory> categories = new ArrayList<>();
    private static final Map<String, RegisteredPlaceholder> placeholders = new ConcurrentHashMap<>();

    static {
        categories.add(new PlayerCategory());
        categories.add(new ServerCategory());
    }

    public static void registerPlaceholder(String identifier,
                                           RegisteredPlaceholder placeholder) {
        placeholders.put(identifier, placeholder);
    }

    public static void registerCategory(PlaceholderCategory category) {
        categories.add(category);
    }

    public static IPlaceholder<String> resolve(String identifier) {
        RegisteredPlaceholder placeholder = null;
        int index = identifier.indexOf('_');
        if (index != -1) {
            String group    = identifier.substring(0, index),
                    value   = identifier.substring(index + 1);
            PlaceholderCategory category =
                    categories.stream().filter(c -> c.getCategory().equals(group))
                    .findAny().orElse(null);
            if (category != null) placeholder = category.get(value);
        }
        if (placeholder != null ||
                (placeholder = placeholders.get(identifier)) != null)
            return new BungeePlaceholder(placeholder);
        return (n, p) -> "<unknown_placeholder>";
    }
}
