package me.megamichiel.animationlib.bungee.category;

import com.google.common.base.Function;
import me.megamichiel.animationlib.bungee.AnimLibPlugin;
import me.megamichiel.animationlib.bungee.RegisteredPlaceholder;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class PlaceholderCategory {

    private final String category;
    private final Map<String, RegisteredPlaceholder> values = new ConcurrentHashMap<>();

    protected PlaceholderCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    protected void put(String value, RegisteredPlaceholder placeholder) {
        values.put(value, placeholder);
    }

    public RegisteredPlaceholder get(String value) {
        return values.get(value);
    }

    protected BungeeCord getServer() {
        return BungeeCord.getInstance();
    }

    public void onEnable(AnimLibPlugin plugin) {}

    public void onDisable() {}
}
