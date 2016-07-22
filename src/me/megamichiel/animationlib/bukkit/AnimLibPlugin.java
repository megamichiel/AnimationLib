package me.megamichiel.animationlib.bukkit;

import me.megamichiel.animationlib.AnimLib;
import me.megamichiel.animationlib.config.ConfigManager;
import me.megamichiel.animationlib.config.type.YamlConfig;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

import static org.bukkit.ChatColor.*;

public class AnimLibPlugin extends JavaPlugin implements Listener, AnimLib {

    private static AnimLibPlugin instance;

    private String update;
    private boolean autoDownloadPlaceholders;

    @Override
    public void onLoad() {
        StringBundle.setAdapter(PapiPlaceholder::new);
    }

    @Override
    public void onEnable() {
        instance = this;
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                String update1 = AnimLib.getVersion(22295);
                if (!update1.equals(getDescription().getVersion())) {
                    getLogger().info("A new version is available: " + update1);
                    update = DARK_GRAY.toString() + '[' + GOLD + "AnimationLib"
                            + DARK_GRAY + ']' + GREEN
                            + " A new version (" + update1 + ") is available";
                }
            } catch (IOException ex) {
                getLogger().warning("Failed to check for updates");
            }
        });
        getServer().getPluginManager().registerEvents(this, this);
        ConfigManager<YamlConfig> config = ConfigManager.of(YamlConfig::new)
                .file(new File(getDataFolder(), "config.yml"));
        config.saveDefaultConfig(() -> getResource("config_bukkit.yml"));
        autoDownloadPlaceholders = config.getConfig().getBoolean("auto-download-placeholders");
    }

    @EventHandler
    void playerJoin(PlayerJoinEvent event) {
        if (update != null
                && event.getPlayer().hasPermission("animlib.seeupdate"))
            event.getPlayer().sendMessage(update);
    }

    public boolean autoDownloadPlaceholders() {
        return autoDownloadPlaceholders;
    }

    public static AnimLibPlugin getInstance() {
        return instance;
    }
}
