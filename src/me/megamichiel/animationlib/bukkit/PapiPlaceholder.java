package me.megamichiel.animationlib.bukkit;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.PlaceholderHook;
import me.clip.placeholderapi.events.PlaceholderHookUnloadEvent;
import me.clip.placeholderapi.expansion.cloud.CloudExpansion;
import me.clip.placeholderapi.expansion.cloud.ExpansionCloudManager;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.PlaceholderContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An IPlaceholder&lt;String&gt; which utilizes the plugin PlaceholderAPI
 */
public class PapiPlaceholder implements IPlaceholder<String> {
    
    public static final boolean apiAvailable;
    
    static {
        boolean flag;
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            flag = true;
        } catch (Exception ex) {
            // No PlaceholderAPI ;c
            flag = false;
        }
        apiAvailable = flag;
    }

    static void registerListener(AnimLibPlugin plugin) {
        plugin.newPipeline(PlaceholderHookUnloadEvent.class)
                .map(PlaceholderHookUnloadEvent::getHook)
                .forEach(hook -> {
                    registry.values().stream().map(pp -> pp.handle)
                            .filter(ref -> ref.get() == hook)
                            .forEach(Reference::clear);
                });
    }

    private static final Map<String, PapiPlaceholder> registry = new ConcurrentHashMap<>();

    public static PapiPlaceholder resolve(String identifier) {
        return registry.computeIfAbsent(identifier, PapiPlaceholder::new);
    }
    
    private final String plugin, name, identifier;
    private WeakReference<PlaceholderHook> handle = new WeakReference<>(null);
    private boolean notified = false, downloading = false;
    
    private PapiPlaceholder(String identifier) {
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

    private PlaceholderHook get() {
        PlaceholderHook hook = handle.get();
        if (hook != null) return hook;
        try {
            hook = PlaceholderAPI.getPlaceholders().get(plugin);
            if (hook != null) {
                handle = new WeakReference<>(hook);
            }
            return hook;
        } catch (NullPointerException ex) { // getPlaceholders() can cause this
            return null;
        }
    }

    @Override
    public String invoke(Nagger nagger, Object who) {
        if (!apiAvailable) return toString();
        PlaceholderHook hook = get();
        if (hook != null) {
            String str = hook.onPlaceholderRequest((Player) who, name);
            return str != null ? str : "<invalid_argument>";
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
                        File file = new File(dir, "Expansion-" + name + ".jar");
                        new BukkitRunnable() {
                            int count = 0;
                            public void run() {
                                if (file.exists()) {
                                    lib.getLogger().info("Successfully downloaded " + plugin + "!");
                                    Bukkit.getScheduler().runTaskLater(lib,
                                            () -> papi.reloadConf(Bukkit.getConsoleSender()), 20L);
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
