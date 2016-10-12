package me.megamichiel.animationlib.bukkit;

import me.megamichiel.animationlib.AnimLib;
import me.megamichiel.animationlib.command.annotation.Alias;
import me.megamichiel.animationlib.command.annotation.CommandHandler;
import me.megamichiel.animationlib.command.ex.InvalidUsageException;
import me.megamichiel.animationlib.command.exec.CommandAdapter;
import me.megamichiel.animationlib.command.exec.TabCompleteContext;
import me.megamichiel.animationlib.config.ConfigManager;
import me.megamichiel.animationlib.config.type.YamlConfig;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Locale.ENGLISH;
import static org.bukkit.ChatColor.*;

public class AnimLibPlugin extends JavaPlugin implements Listener, AnimLib, CommandAdapter {

    private static AnimLibPlugin instance;

    private String update;
    private boolean autoDownloadPlaceholders;

    private final BukkitCommandAPI commandAPI = new BukkitCommandAPI();

    @Override
    public void onLoad() {
        StringBundle.setAdapter(PapiPlaceholder::new);
    }

    @Override
    public void onEnable() {
        instance = this;
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                String update = AnimLib.getVersion(22295);
                if (!update.equals(getDescription().getVersion())) {
                    getLogger().info("A new version is available: " + update);
                    this.update = DARK_GRAY.toString() + '[' + GOLD + "AnimationLib"
                            + DARK_GRAY + ']' + GREEN
                            + " A new version (" + update + ") is available";
                    PipelineListener.newPipeline(PlayerJoinEvent.class, this)
                            .map(PlayerEvent::getPlayer)
                            .filter(p -> p.hasPermission("animlib.seeupdate"))
                            .forEach(p -> p.sendMessage(this.update));
                }
            } catch (IOException ex) {
                getLogger().warning("Failed to check for updates");
            }
        });
        ConfigManager<YamlConfig> config = ConfigManager.of(YamlConfig::new)
                .file(new File(getDataFolder(), "config.yml"));
        config.saveDefaultConfig(() -> getResource("config_bukkit.yml"));
        autoDownloadPlaceholders = config.getConfig().getBoolean("auto-download-placeholders");

        commandAPI.registerCommands(this, this);
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

    @CommandHandler(value = "memes", usage = "/<command> <world...> OR /<command> test <message...> OR /<command> help")
    void memesCommand(CommandSender sender, World[] worlds) {
        sender.sendMessage("Worlds: " + Arrays.toString(worlds) + "!");
    }

    @Alias("memes")
    String memesCommand(CommandSender sender, @Alias("@info") String label,
                        String identifier, String[] test) {
        if (!"test".equalsIgnoreCase(identifier))
            throw new InvalidUsageException();
        return "You entered: " + label + ' ' + Arrays.toString(test);
    }

    @Alias("memes")
    boolean memesCommand(CommandSender sender, String identifier) {
        if (!"help".equalsIgnoreCase(identifier))
            throw new InvalidUsageException();
        return false; // Returns command usage, v sneaky ;3
    }

    @Alias("memes")
    void memesCommand(TabCompleteContext<CommandSender, Command> ctx) {
        List<String> list = new ArrayList<>();
        String[] args = ctx.getArgs();
        if (args.length <= 1) {
            getServer().getWorlds().stream().map(World::getName).forEach(list::add);
            list.add("test");
            list.add("help");
            if (args.length == 1) {
                String s = args[0].toLowerCase(ENGLISH);
                list.removeIf(str -> !str.toLowerCase(ENGLISH).startsWith(s));
            }
        } else {
            String lower = args[0].toLowerCase(ENGLISH);
            if (!"help".equals(lower) && !"test".equals(lower)) {
                String s = args[args.length - 1].toLowerCase(ENGLISH);
                getServer().getWorlds().stream().map(World::getName)
                        .filter(str -> str.toLowerCase(ENGLISH).startsWith(s)).forEach(list::add);
            }
        }
        ctx.setCompletions(list);
    }
}
