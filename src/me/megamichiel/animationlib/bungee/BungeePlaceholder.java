package me.megamichiel.animationlib.bungee;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.bungee.category.*;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.PlaceholderContext;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BungeePlaceholder implements IPlaceholder<String> {

    private final String identifier;
    private final RegisteredPlaceholder placeholder;

    public BungeePlaceholder(String identifier) {
        this(identifier, placeholders.get(identifier));
    }

    public BungeePlaceholder(String identifier, RegisteredPlaceholder placeholder) {
        this.identifier = identifier;
        this.placeholder = placeholder;
    }

    @Override
    public String invoke(Nagger nagger, Object who) {
        return placeholder.invoke(nagger, (ProxiedPlayer) who);
    }

    @Override
    public String invoke(Nagger nagger, Object who, PlaceholderContext ctx) {
        return ctx == null ? invoke(nagger, who) : ctx.invoke(who, identifier, this);
    }

    private static final List<PlaceholderCategory> categories = new ArrayList<>();
    private static final Map<String, RegisteredPlaceholder> placeholders = new ConcurrentHashMap<>();

    static {
        categories.add(new PingerCategory());
        categories.add(new JavaScriptCategory());
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
                   value    = identifier.substring(index + 1);
            PlaceholderCategory category =
                    categories.stream().filter(c -> c.getCategory().equals(group))
                    .findAny().orElse(null);
            if (category != null) placeholder = category.get(value);
        }
        if (placeholder != null || (placeholder = placeholders.get(identifier)) != null)
            return new BungeePlaceholder(identifier, placeholder);
        return (n, p) -> "<unknown_placeholder>";
    }

    static void onLoad(AnimLibPlugin plugin) {
        categories.add(new PlayerCategory(plugin));
        categories.add(new ServerCategory(plugin));
    }

    static void onEnable(AnimLibPlugin plugin) {
        categories.forEach(c -> c.onEnable(plugin));
    }
}
