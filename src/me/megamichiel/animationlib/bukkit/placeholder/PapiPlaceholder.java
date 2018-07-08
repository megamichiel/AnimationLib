package me.megamichiel.animationlib.bukkit.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;
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

/**
 * An IPlaceholder&lt;String&gt; which utilizes the plugin PlaceholderAPI
 */
public class PapiPlaceholder implements IPlaceholder<String> {

    public static boolean register(Plugin plugin, String identifier, BiFunction<Player, String, String> func) {
        return apiAvailable && new PlaceholderHook() {

            final boolean hooked = PlaceholderAPI.registerPlaceholderHook(identifier == null
                    ? plugin.getName().toLowerCase(Locale.ENGLISH) : identifier, this);

            @Override
            public String onPlaceholderRequest(Player player, String s) {
                return func.apply(player, s);
            }
        }.hooked;
    }
    
    public static final boolean apiAvailable;
    
    static {
        boolean flag;
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            flag = true;
        } catch (ClassNotFoundException ex) {
            // No PlaceholderAPI ;c
            flag = false;
        }
        apiAvailable = flag;
    }

    private static final Map<String, PapiPlaceholder> registry = new ConcurrentHashMap<>();

    public static PapiPlaceholder resolve(String identifier) {
        return registry.computeIfAbsent(identifier, PapiPlaceholder::new);
    }
    
    private final String plugin, name, identifier;
    private final boolean color;
    private boolean notified = false;
    
    private PapiPlaceholder(String identifier) {
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
        if (!apiAvailable) {
            return toString();
        }

        Map<String, PlaceholderHook> map = PlaceholderAPI.getPlaceholders();

        if (map == null) {
            return "<unknown_placeholder>";
        }

        Object hook = map.get(plugin);
        if (hook != null) {
            String str;
            try {
                str = ((PlaceholderHook) hook).onRequest((Player) who, name);
            } catch (Exception ex) {
                nagger.nag("Failed to process placeholder %" + identifier + "%!");
                nagger.nag(ex);
                return "<internal_error>";
            }
            return str != null ? (color ? StringBundle.colorAmpersands(str) : str) : "<invalid_argument>";
        } else {
            if (!notified) {
                nagger.nag("Couldn't find placeholder by ID \"" + plugin + "\"!");
                notified = true;
            }
        }
        return "<unknown_placeholder>";
    }
}
