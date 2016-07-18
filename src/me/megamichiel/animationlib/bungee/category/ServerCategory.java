package me.megamichiel.animationlib.bungee.category;

import me.megamichiel.animationlib.bungee.AnimLibPlugin;

public class ServerCategory extends PlaceholderCategory {

    private static final long MB = 1024 * 1024;

    public ServerCategory() {
        super("server");

        put("online", (n, p) -> Integer.toString(getServer().getOnlineCount()));
        put("uptime", (n, p) -> AnimLibPlugin.inst().uptime());

        Runtime runtime = Runtime.getRuntime();
        put("ram_used",  (n, p) -> Long.toString((runtime.totalMemory() - runtime.freeMemory()) / MB));
        put("ram_free",  (n, p) -> Long.toString(runtime.freeMemory() / MB));
        put("ram_total", (n, p) -> Long.toString(runtime.totalMemory() / MB));
        put("ram_max",   (n, p) -> Long.toString(runtime.maxMemory() / MB));
    }
}
