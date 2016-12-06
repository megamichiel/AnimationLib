package me.megamichiel.animationlib.bungee.category;

import me.megamichiel.animationlib.bungee.AnimLibPlugin;
import me.megamichiel.animationlib.bungee.RegisteredPlaceholder;

public class PlayerCategory extends PlaceholderCategory {

    private final AnimLibPlugin plugin;

    public PlayerCategory(AnimLibPlugin plugin) {
        super("player");
        this.plugin = plugin;

        put("name",         (n, p) -> p.getName());
        put("server",       (n, p) -> p.getServer().getInfo().getName());
        put("displayname",  (n, p) -> p.getDisplayName());
        put("ip",           (n, p) -> p.getAddress().getAddress().getHostAddress());
        put("uuid",         (n, p) -> p.getUniqueId().toString());
        put("ping",         (n, p) -> Integer.toString(p.getPing()));
        put("locale",       (n, p) -> p.getLocale().toString());
    }

    @Override
    public RegisteredPlaceholder get(String value) {
        RegisteredPlaceholder result = super.get(value);
        if (result != null) return result;
        if (value.startsWith("has_permission_")) {
            String perm = value.substring(15);
            return (n, p) -> bool(p.hasPermission(perm));
        }
        if (value.startsWith("has_mod_")) {
            String mod = value.substring(8);
            return (n, p) -> bool(p.getModList().containsKey(mod));
        }
        if (value.startsWith("mod_version_")) {
            String mod = value.substring(12);
            return (n, p) -> {
                String s = p.getModList().get(mod);
                return s == null ? bool(false) : s;
            };
        }
        return null;
    }

    private String bool(boolean flag) {
        return flag ? plugin.booleanTrue() : plugin.booleanFalse();
    }
}
