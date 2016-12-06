package me.megamichiel.animationlib.bukkit;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import org.bukkit.entity.Player;

public class AnimLibPlaceholders extends PlaceholderHook {

    public static void init(AnimLibPlugin plugin) {
        PlaceholderAPI.registerPlaceholderHook("animlib", new AnimLibPlaceholders(plugin));
    }

    private final AnimLibPlugin plugin;

    private AnimLibPlaceholders(AnimLibPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String onPlaceholderRequest(Player player, String arg) {
        if (arg.startsWith("formula_")) {
            IPlaceholder<String> formula = plugin.getFormula(arg.substring(8));
            return formula == null ? null : formula.invoke(plugin, player);
        }
        return null;
    }
}
