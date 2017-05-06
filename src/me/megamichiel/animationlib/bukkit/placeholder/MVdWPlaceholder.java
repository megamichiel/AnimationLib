package me.megamichiel.animationlib.bukkit.placeholder;

import be.maximvdw.placeholderapi.PlaceholderAPI;
import be.maximvdw.placeholderapi.PlaceholderReplaceEvent;
import be.maximvdw.placeholderapi.PlaceholderReplacer;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.PlaceholderContext;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MVdWPlaceholder implements IPlaceholder<String> {

    private static final Map<String, MVdWPlaceholder> registry = new ConcurrentHashMap<>();

    public static MVdWPlaceholder resolve(String identifier) {
        return registry.computeIfAbsent(identifier, MVdWPlaceholder::new);
    }

    private final String plugin, name, identifier;

    private MVdWPlaceholder(String identifier) {
        int index = (this.identifier = identifier).indexOf('_');
        if (index > 0) {
            plugin = identifier.substring(0, index);
            name = identifier.substring(index + 1);
        } else {
            plugin = "";
            name = identifier;
        }
    }

    @Override
    public String toString() {
        return '%' + identifier + '%';
    }

    @Override
    public String invoke(Nagger nagger, Object who, PlaceholderContext ctx) {
        return ctx != null ? ctx.invoke(who, identifier, this) : invoke(nagger, who);
    }

    @Override
    public String invoke(Nagger nagger, Object who) {
        PlaceholderReplacer replacer = PlaceholderAPI.getCustomPlaceholders().get(plugin);
        return replacer != null ? replacer.onPlaceholderReplace(new PlaceholderReplaceEvent((Player) who, name)) : "<unknown_placeholder>";
    }
}
