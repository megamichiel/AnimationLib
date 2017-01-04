package me.megamichiel.animationlib.bukkit;

import me.megamichiel.animationlib.AnimLib;
import me.megamichiel.animationlib.config.ConfigManager;
import me.megamichiel.animationlib.config.type.YamlConfig;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.animationlib.util.db.DataBase;
import me.megamichiel.animationlib.util.pipeline.Pipeline;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

import static org.bukkit.ChatColor.*;

public class AnimLibPlugin extends JavaPlugin implements Listener, AnimLib<Event> {

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
        config.file(new File(getDataFolder(), "config.yml"))
                .saveDefaultConfig(() -> getResource("config_bukkit.yml"));

        if (PapiPlaceholder.apiAvailable)
            placeholders = AnimLibPlaceholders.init(this);

        loadConfig();

        /*commandAPI.registerCommand(this, new CommandInfo("itemmeta", ctx -> {
            Player sender = (Player) ctx.getSender(); // Fak type checking
            ItemTag tag = new ItemTag();
            tag.setDisplayName(ChatColor.GOLD + "Fancy name");
            tag.setLore(Arrays.asList("Dank line", "Another dank line", ChatColor.GOLD + "Golden dank line"));
            tag.setUnbreakable(true);
            tag.setSkullOwner(sender.getName());
            tag.addEnchant(Enchantment.DIG_SPEED, 5, false);
            tag.setCanDestroy(new HashSet<>(Arrays.asList("minecraft:stone", "minecraft:cobblestone")));
            sender.getInventory().setItemInMainHand(tag.toItemStack(Material.DIAMOND_PICKAXE));
        }));*/

        /*DataBase db = DataBase.getDataBase("jdbc:mysql://localhost/test").as("host", "");
        PipelineListener.newPipeline(PlayerJoinEvent.class, this)
                .map(PlayerEvent::getPlayer).post(true).map(player -> {
                    try {
                        Connection con = db.getConnection();
                        try (Statement sm = con.createStatement();
                             ResultSet res = sm.executeQuery("SELECT * FROM `test` WHERE `UUID`='" + player.getUniqueId() + "'")) {
                            Runnable task;
                            if (res.next()) {
                                int joins  = res.getInt("Joins") + 1;
                                String msg = GREEN + "Joins: " + joins;
                                String name = res.getString("Name");
                                if (!name.equals(player.getName())) {
                                    msg += ", and you changed name from " + name + "!";
                                    name = ",`Name`=?";
                                } else name = "";
                                final String send = msg;
                                task = () -> player.sendMessage(send);
                                try (PreparedStatement ps = con.prepareStatement("UPDATE `test` SET `Joins`=?" + name + " WHERE `UUID`=?")) {
                                    ps.setObject(1, Integer.toString(joins));
                                    if (!name.isEmpty()) {
                                        ps.setObject(2, player.getName());
                                        ps.setObject(3, player.getUniqueId().toString());
                                    } else ps.setObject(2, player.getUniqueId().toString());
                                    ps.executeUpdate();
                                }
                            } else {
                                task = () -> player.sendMessage(RED + "No entries found ;c");
                                try (PreparedStatement ps = con.prepareStatement("INSERT INTO `test`(`UUID`, `Name`, `Joins`) VALUES (?,?,?)")) {
                                    ps.setObject(1, player.getUniqueId().toString());
                                    ps.setObject(2, player.getName());
                                    ps.setObject(3, "1");
                                    ps.executeUpdate();
                                }
                            }
                            return task;
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                    return null;
                }).nonNull().post(false).forEach(Runnable::run);*/
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
