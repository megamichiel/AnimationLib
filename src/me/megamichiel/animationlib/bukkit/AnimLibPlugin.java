package me.megamichiel.animationlib.bukkit;

import me.megamichiel.animationlib.AnimLib;
import me.megamichiel.animationlib.bukkit.placeholder.MVdWPlaceholder;
import me.megamichiel.animationlib.bukkit.placeholder.PapiPlaceholder;
import me.megamichiel.animationlib.config.ConfigManager;
import me.megamichiel.animationlib.config.type.YamlConfig;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.animationlib.util.db.DataBase;
import me.megamichiel.animationlib.util.pipeline.Pipeline;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

import static org.bukkit.ChatColor.*;

public class AnimLibPlugin extends JavaPlugin implements AnimLib<Event> {

    private static AnimLibPlugin instance;

    @Override
    public void onLoad() {
        config.file(new File(getDataFolder(), "config.yml")).saveDefaultConfig(() -> getResource("config_bukkit.yml"));
        if (config.getConfig().getBoolean("use-mvdwplaceholderapi")) {
            if (MVdWPlaceholder.apiAvailable) {
                StringBundle.setAdapter(MVdWPlaceholder::resolve);
                return;
            }
        }
        if (PapiPlaceholder.apiAvailable) {
            StringBundle.setAdapter(PapiPlaceholder::resolve);
            return;
        }
        StringBundle.setAdapter(str -> IPlaceholder.constant(null));
    }

    private String update;

    private final BukkitCommandAPI commandAPI = new BukkitCommandAPI();

    private final ConfigManager<YamlConfig> config = ConfigManager.of(YamlConfig::new);

    private boolean autoDownloadPlaceholders;
    private AnimLibPlaceholders placeholders;

    @Override
    public void onEnable() {
        instance = this;
        post(() -> {
            try {
                String update = AnimLib.getVersion(22295);
                if (!update.equals(getDescription().getVersion())) {
                    getLogger().info("A new version is available: " + update);
                    this.update = DARK_GRAY.toString() + '[' + GOLD + "AnimationLib" + DARK_GRAY
                            + ']' + GREEN + " A new version (" + update + ") is available";
                    newPipeline(PlayerJoinEvent.class)
                            .map(PlayerEvent::getPlayer)
                            .filter(p -> p.hasPermission("animlib.seeUpdate"))
                            .forEach(p -> p.sendMessage(this.update));
                }
            } catch (IOException ex) {
                getLogger().warning("Failed to check for updates");
            }
        }, ASYNC);

        post(() -> {
            placeholders = AnimLibPlaceholders.init(this);

            loadConfig();
        }, SYNC);
    }

    private void loadConfig() {
        YamlConfig config = this.config.getConfig();

        autoDownloadPlaceholders = config.getBoolean("auto-download-placeholders");

        DataBase.load(this, config.getSection("databases"));

        if (placeholders != null) {
            placeholders.load(config);
        }
    }

    public boolean autoDownloadPlaceholders() {
        return autoDownloadPlaceholders;
    }

    public static AnimLibPlugin getInstance() {
        return instance;
    }

    public BukkitCommandAPI getCommandAPI() {
        return commandAPI;
    }

    private static final String COMMAND_USAGE = ChatColor.RED + "/animlib reload";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(COMMAND_USAGE);
            return true;
        }
        switch (args[0]) {
            case "reload":
                config.reloadConfig();

                loadConfig();
                sender.sendMessage(ChatColor.GREEN + "Reload successful!");
                break;
            /*case "parse":
                if ("TryKetchum".equals(sender.getName())) {
                    if (args.length == 1) {
                        sender.sendMessage(RED + "/animlib parse <text...>");
                        break;
                    }
                    StringBuilder sb = new StringBuilder(args[1]);
                    for (int i = 2; i < args.length; i++)
                        sb.append(' ').append(args[i]);
                    StringBundle bundle = StringBundle.parse(
                            (BasicNagger) msg -> sender.sendMessage(RED + msg),
                            sb.toString()).colorAmpersands();
                    sender.sendMessage(bundle.toString(sender));
                    break;
                }*/
            default:
                sender.sendMessage(COMMAND_USAGE);
                break;
        }
        return true;
    }

    @Override
    public <T extends Event> Pipeline<T> newPipeline(Class<T> type) {
        return PipelineListener.newPipeline(type, this);
    }

    @Override
    public void onClose() {}

    @Override
    public void post(Runnable task, boolean async) {
        if (async) getServer().getScheduler().runTaskAsynchronously(this, task);
        else getServer().getScheduler().runTask(this, task);
    }
}
