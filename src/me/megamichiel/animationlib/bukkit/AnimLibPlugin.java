package me.megamichiel.animationlib.bukkit;

import me.megamichiel.animationlib.AnimLib;
import me.megamichiel.animationlib.bukkit.nbt.ItemTag;
import me.megamichiel.animationlib.command.CommandInfo;
import me.megamichiel.animationlib.command.exec.CommandContext;
import me.megamichiel.animationlib.command.exec.CommandExecutor;
import me.megamichiel.animationlib.config.ConfigManager;
import me.megamichiel.animationlib.config.type.YamlConfig;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.animationlib.util.db.DataBase;
import me.megamichiel.animationlib.util.pipeline.Pipeline;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.bukkit.ChatColor.*;

public class AnimLibPlugin extends JavaPlugin implements AnimLib<Event> {

    private static AnimLibPlugin instance;

    @Override
    public void onLoad() {
        StringBundle.setAdapter(PapiPlaceholder::resolve);
    }

    private String update;

    private final BukkitCommandAPI commandAPI = new BukkitCommandAPI();

    private final ConfigManager<YamlConfig> config = ConfigManager.of(YamlConfig::new);

    private boolean autoDownloadPlaceholders;
    private AnimLibPlaceholders placeholders;

    @Override
    public void onEnable() {
        instance = this;
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
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
        });
        config.file(new File(getDataFolder(), "config.yml"))
                .saveDefaultConfig(() -> getResource("config_bukkit.yml"));

        if (PapiPlaceholder.apiAvailable) {
            placeholders = AnimLibPlaceholders.init(this);
            PapiPlaceholder.registerListener(this);
        }

        loadConfig();

        BukkitCommandAPI.getInstance().registerCommand(this, new CommandInfo("items", new CommandExecutor() {

            private void bukkitAPI(Player player) {
                ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + "Very diamond!1!");
                meta.setLore(Arrays.asList(ChatColor.AQUA + "Fabulous lore", ChatColor.RED + "Red line"));
                meta.addEnchant(Enchantment.DAMAGE_ALL, 3, false);
                item.setItemMeta(meta);
                player.getInventory().setItem(2, item);
            }

            private void reflectNMS(Player player) {
                ItemTag item = new ItemTag();
                item.setDisplay(new ItemTag.Display(ChatColor.GREEN + "Very diamond!1!",
                        Arrays.asList(ChatColor.AQUA + "Fabulous lore", ChatColor.RED + "Red line")));
                item.setEnchants(new ItemTag.Ench().set(Enchantment.DAMAGE_ALL, 3));
                player.getInventory().setItem(2, item.toItemStack(Material.DIAMOND_SWORD));
            }

            @Override
            public void onCommand(CommandContext ctx) {
                Object sender = ctx.getSender();
                if (!(sender instanceof Player)) {
                    ctx.sendMessage(ChatColor.RED + "You must be a player for this!");
                    return;
                }
                Player player = (Player) sender;
                int warmup = 1000, count = 10000;
                long time;
                for (int i = 0; i < warmup; i++) {
                    bukkitAPI(player);
                }
                time = System.currentTimeMillis();
                for (int i = 0; i < count; i++) {
                    bukkitAPI(player);
                }
                player.sendMessage("Bukkit API: " + (System.currentTimeMillis() - time));

                for (int i = 0; i < warmup; i++) {
                    reflectNMS(player);
                }
                time = System.currentTimeMillis();
                for (int i = 0; i < count; i++) {
                    reflectNMS(player);
                }
                player.sendMessage("Reflect NMS: " + (System.currentTimeMillis() - time));
            }
        }));
    }

    private void loadConfig() {
        config.reloadConfig();

        YamlConfig config = this.config.getConfig();

        autoDownloadPlaceholders = config.getBoolean("auto-download-placeholders");

        DataBase.load(this, config.getSection("databases"));

        if (PapiPlaceholder.apiAvailable)
            placeholders.load(config);
    }

    boolean autoDownloadPlaceholders() {
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
