package me.megamichiel.animationlib.bukkit.placeholder;

import be.maximvdw.placeholderapi.PlaceholderAPI;
import be.maximvdw.placeholderapi.PlaceholderReplaceEvent;
import be.maximvdw.placeholderapi.PlaceholderReplacer;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.PlaceholderContext;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class MVdWPlaceholder implements IPlaceholder<String> {

    public static boolean register(Plugin plugin, String identifier, BiFunction<Player, String, String> func) {
        return apiAvailable && PlaceholderAPI.registerPlaceholder(plugin, identifier == null ? plugin.getName().toLowerCase(Locale.ENGLISH) : identifier, evt -> func.apply(evt.getPlayer(), evt.getPlaceholder()));
    }

    public static final boolean apiAvailable;

    static {
        boolean flag;
        try {
            Class.forName("be.maximvdw.placeholderapi.PlaceholderAPI");
            flag = true;
        } catch (ClassNotFoundException ex) {
            flag = false;
        }
        apiAvailable = flag;
    }

    private static final Map<String, MVdWPlaceholder> registry = new ConcurrentHashMap<>();

    public static MVdWPlaceholder resolve(String identifier) {
        return registry.computeIfAbsent(identifier, MVdWPlaceholder::new);
    }

    private final String plugin, name, identifier;
    private final boolean color;

    private MVdWPlaceholder(String identifier) {
        if (color = identifier.startsWith("color:")) {
            identifier = identifier.substring(6);
        }

        int index = (this.identifier = identifier).indexOf('_');

        plugin = index == -1 ? "" : identifier.substring(0, index);
        name = identifier.substring(index + 1);
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
        try {
            if (replacer == null) {
                return "<unknown_placeholder>";
            }
            String str = replacer.onPlaceholderReplace(new PlaceholderReplaceEvent((Player) who, name));

            return color ? StringBundle.colorAmpersands(str) : str;
        } catch (Exception ex) {
            nagger.nag("Failed to process placeholder %" + identifier + "%!");
            nagger.nag(ex);
            return "<internal_error>";
        }
    }
}
