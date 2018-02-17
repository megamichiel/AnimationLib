package me.megamichiel.animationlib.bukkit.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.PlaceholderHook;
import me.clip.placeholderapi.expansion.cloud.CloudExpansion;
import me.clip.placeholderapi.expansion.cloud.ExpansionCloudManager;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.bukkit.AnimLibPlugin;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.PlaceholderContext;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.animationlib.util.ReflectClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
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
    private static final ReflectClass.Field placeholders;
    
    static {
        boolean flag;
        ReflectClass.Field field;
        try {
            field = new ReflectClass("me.clip.placeholderapi.PlaceholderAPI").getField("placeholders").makeAccessible();
            flag = true;
        } catch (ReflectClass.ReflectException ex) {
            // No PlaceholderAPI ;c
            flag = false;
            field = null;
        }
        apiAvailable = flag;
        placeholders = field;
    }

    private static final Map<String, PapiPlaceholder> registry = new ConcurrentHashMap<>();

    public static PapiPlaceholder resolve(String identifier) {
        return registry.computeIfAbsent(identifier, PapiPlaceholder::new);
    }
    
    private final String plugin, name, identifier;
    private final boolean color;
    private boolean notified = false, downloading = false;
    
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

        Map map = (Map) placeholders.getStatic();

        if (map == null) {
            return "<unknown_placeholder>";
        }

        Object hook = map.get(plugin);
        if (hook != null) {
            String str;
            try {
                str = ((PlaceholderHook) hook).onPlaceholderRequest((Player) who, name);
            } catch (Exception ex) {
                nagger.nag("Failed to process placeholder %" + identifier + "%!");
                nagger.nag(ex);
                return "<internal_error>";
            }
            return str != null ? (color ? StringBundle.colorAmpersands(str) : str) : "<invalid_argument>";
        } else {
            AnimLibPlugin lib = AnimLibPlugin.getInstance();
            if (!downloading && lib.autoDownloadPlaceholders()) {
                PlaceholderAPIPlugin papi = PlaceholderAPIPlugin.getInstance();
                if (papi.getExpansionManager().getExpansion(plugin) == null) {
                    ExpansionCloudManager manager = papi.getExpansionCloud();
                    CloudExpansion expansion = manager.getCloudExpansion(plugin);
                    if (expansion != null) {
                        downloading = true;
                        lib.getLogger().info("Attempting to download expansion " + plugin + "...");
                        manager.downloadExpansion(null, expansion);
                        File dir = new File(papi.getDataFolder(), "expansions");
                        File file = new File(dir, "Expansion-" + expansion.getName() + ".jar");
                        new BukkitRunnable() {
                            int count = 0;
                            @Override
                            public void run() {
                                if (file.exists()) {
                                    lib.getLogger().info("Successfully downloaded " + plugin + "!");
                                    Bukkit.getScheduler().runTaskLater(lib, () -> papi.reloadConf(Bukkit.getConsoleSender()), 20L);
                                    cancel();
                                } else if (++count == 10) {
                                    lib.getLogger().warning("Unable to download expansion " + plugin + "!");
                                    cancel();
                                }
                            }
                        }.runTaskTimer(lib, 40L, 40L);
                    }
                } else downloading = true;
            }
            if (!downloading && !notified) {
                nagger.nag("Couldn't find placeholder by ID \"" + plugin + "\"!");
                notified = true;
            }
        }
        return downloading ? "<downloading>" : "<unknown_placeholder>";
    }
}
