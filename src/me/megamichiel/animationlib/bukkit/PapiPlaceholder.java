package me.megamichiel.animationlib.bukkit;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.PlaceholderHook;
import me.clip.placeholderapi.expansion.cloud.CloudExpansion;
import me.clip.placeholderapi.expansion.cloud.ExpansionCloudManager;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Map.Entry;

/**
 * An IPlaceholder&lt;String&gt; which utilises the plugin PlaceholderAPI
 *
 */
public class PapiPlaceholder implements IPlaceholder<String> {
    
    private static final boolean placeHolder;
    
    static {
        boolean flag = false;
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            flag = true;
        } catch (Exception ex) {
            // No PlaceholderAPI ;c
        }
        placeHolder = flag;
    }
    
    private final String plugin;
    private final String name;
    private PlaceholderHook handle;
    private boolean notified = false, downloading = false;
    
    public PapiPlaceholder(String identifier) {
        int index = identifier.indexOf('_');
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
        return "%" + plugin + "_" + name + "%";
    }
    
    @Override
    public String invoke(Nagger nagger, Object who) {
        if (!placeHolder) return toString();
        if (handle != null || getPlaceholder()) {
            String str = handle.onPlaceholderRequest((Player) who, name);
            return str != null ? str : "<invalid_argument>";
        } else {
            AnimLibPlugin lib = AnimLibPlugin.getInstance();
            if (!downloading && lib.autoDownloadPlaceholders()) {
                PlaceholderAPIPlugin papi = PlaceholderAPIPlugin.getInstance();
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
                            } else if (count++ == 10) {
                                lib.getLogger().warning("Unable to download expansion " + plugin + "!");
                                cancel();
                            }
                        }
                    }.runTaskTimer(lib, 40L, 40L);
                }
            }
            if (handle == null && !downloading && !notified) {
                nagger.nag("Couldn't find placeholder by ID \"" + plugin + "\"!");
                notified = true;
            }
        }
        return "<unknown_placeholder>";
    }

    private boolean getPlaceholder() {
        for (Entry<String, PlaceholderHook> entry : PlaceholderAPI.getPlaceholders().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(this.plugin)) {
                handle = entry.getValue();
                return true;
            }
        }
        return false;
    }
}
