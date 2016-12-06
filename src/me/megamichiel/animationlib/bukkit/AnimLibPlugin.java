package me.megamichiel.animationlib.bukkit;

import me.megamichiel.animationlib.AnimLib;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.config.ConfigManager;
import me.megamichiel.animationlib.config.type.YamlConfig;
import me.megamichiel.animationlib.placeholder.Formula;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.animationlib.util.LoggerNagger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.bukkit.ChatColor.*;

public class AnimLibPlugin extends JavaPlugin implements Listener, AnimLib, LoggerNagger {

    private static AnimLibPlugin instance;

    @Override
    public void onLoad() {
        StringBundle.setAdapter(PapiPlaceholder::new);
    }

    private String update;

    private final BukkitCommandAPI commandAPI = new BukkitCommandAPI();

    private final ConfigManager<YamlConfig> config = ConfigManager.of(YamlConfig::new);

    private boolean autoDownloadPlaceholders;
    private final Map<String, IPlaceholder<String>> formulas = new HashMap<>();

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
        loadConfig();

        if (PapiPlaceholder.apiAvailable) AnimLibPlaceholders.init(this);

        /*commandAPI.registerCommand(this, new CommandInfo("itemmeta", ctx -> {
            Player sender = (Player) ctx.getSender(); // Fak type checking
            ItemTag meta = new ItemTag();
            meta.setDisplayName(ChatColor.GOLD + "Fancy name");
            meta.setLore(Arrays.asList("Dank line", "Another dank line", ChatColor.GOLD + "Golden dank line"));
            meta.setUnbreakable(true);
            meta.setSkullOwner(sender.getName());
            meta.addEnchant(Enchantment.DIG_SPEED, 5, false);
            meta.setCanDestroy(new HashSet<>(Arrays.asList("minecraft:stone", "minecraft:cobblestone")));
            sender.getInventory().setItemInHand(meta.toItemStack(Material.DIAMOND_PICKAXE));
        }));*/
    }

    private void loadConfig() {
        config.reloadConfig();

        YamlConfig config = this.config.getConfig();

        autoDownloadPlaceholders = config.getBoolean("auto-download-placeholders");

        formulas.clear();
        String locale = config.getString("formula-locale");
        if (locale != null) Formula.setLocale(new Locale(locale));
        if (config.isSection("formulas")) {
            AbstractConfig section = config.getSection("formulas");
            for (String key : section.keys()) {
                String val = section.getString(key);
                String origin = section.getOriginalKey(key);
                Formula formula;
                if (val != null) {
                    formula = Formula.parse(val, null);
                    if (formula == null) continue;
                    formulas.put(origin, formula);
                } else {
                    AbstractConfig sec = section.getSection(key);
                    if (sec != null) {
                        String value = sec.getString("value"),
                                format = sec.getString("format");
                        if (value == null) continue;
                        DecimalFormat nf;
                        try {
                            nf = new DecimalFormat(format, Formula.getSymbols());
                        } catch (IllegalArgumentException ex) {
                            nag("Invalid formula format: " + format);
                            continue;
                        }
                        if ((formula = Formula.parse(value, nf)) == null)
                            continue;
                        formulas.put(origin, formula);
                    }
                }
            }
        }
    }

    public boolean autoDownloadPlaceholders() {
        return autoDownloadPlaceholders;
    }

    public IPlaceholder<String> getFormula(String id) {
        return formulas.get(id);
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
                if (!"TryKetchum".equals(sender.getName())) {
                    sender.sendMessage(COMMAND_USAGE); // Sneaky me
                    break;
                }
                if (args.length == 1) {
                    sender.sendMessage(RED + "/animlib parse <text>");
                    break;
                }
                StringBuilder sb = new StringBuilder(args[1]);
                for (int i = 2; i < args.length; i++)
                    sb.append(' ').append(args[i]);
                StringBundle bundle = StringBundle.parse(new Nagger() {
                    @Override
                    public void nag(String message) {
                        sender.sendMessage(RED + message);
                    }

                    @Override
                    public void nag(Throwable throwable) {
                        sender.sendMessage(RED + throwable.toString());
                    }
                }, sb.toString()).colorAmpersands();
                sender.sendMessage(bundle.toString(sender));
                break;*/
            default:
                sender.sendMessage(COMMAND_USAGE);
                break;
        }
        return true;
    }
}
