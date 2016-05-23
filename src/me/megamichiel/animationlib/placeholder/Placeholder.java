package me.megamichiel.animationlib.placeholder;

import java.util.Map.Entry;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;

import me.megamichiel.animationlib.Nagger;
import org.bukkit.entity.Player;

/**
 * An IPlaceholder&lt;String&gt; which utilises the plugin PlaceholderAPI
 *
 */
public class Placeholder implements IPlaceholder<String> {
    
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
    private boolean notified = false;
    
    public Placeholder(String identifier) {
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
    public String invoke(Nagger nagger, Player who) {
        if (!placeHolder) return toString();
        if (handle == null) {
            for (Entry<String, PlaceholderHook> entry : PlaceholderAPI.getPlaceholders().entrySet()) {
                if (entry.getKey().equalsIgnoreCase(this.plugin)) {
                    handle = entry.getValue();
                    break;
                }
            }
        }
        if (handle != null) {
            String str = handle.onPlaceholderRequest(who, name);
            return str != null ? str : "<invalid_argument>";
        } else if (!notified) {
            nagger.nag("Couldn't find placeholder by ID \"" + plugin + "\"!");
            notified = true;
        }
        return "<unknown_placeholder>";
    }
}
