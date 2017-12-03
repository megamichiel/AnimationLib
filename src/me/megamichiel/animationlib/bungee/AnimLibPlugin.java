package me.megamichiel.animationlib.bungee;

import me.megamichiel.animationlib.AnimLib;
import me.megamichiel.animationlib.config.ConfigManager;
import me.megamichiel.animationlib.config.type.YamlConfig;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.animationlib.util.db.DataBase;
import me.megamichiel.animationlib.util.pipeline.Pipeline;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static me.megamichiel.animationlib.placeholder.StringBundle.colorAmpersands;

public class AnimLibPlugin extends Plugin implements AnimLib<Event> {

    private String booleanTrue, booleanFalse;
    private final long startTime = System.currentTimeMillis();

    private final BungeeCommandAPI commandAPI = new BungeeCommandAPI(this);

    @Override
    public void onLoad() {
        BungeePlaceholder.onLoad(this);
        StringBundle.setAdapter(BungeePlaceholder::resolve);
    }

    @Override
    public void onEnable() {
        BungeePlaceholder.onEnable(this);
        getProxy().getScheduler().runAsync(this, () -> {
            try {
                String update = AnimLib.getVersion(22295);
                if (!update.equals(getDescription().getVersion())) {
                    getLogger().info("A new version is available: " + update);
                }
            } catch (IOException ex) {
                getLogger().warning("Failed to check for updates");
            }
        });
        ConfigManager<YamlConfig> config = ConfigManager.of(YamlConfig::new)
                .file(new File(getDataFolder(), "config.yml"));
        config.saveDefaultConfig(() -> getResourceAsStream("config_bungee.yml"));
        YamlConfig cfg = config.getConfig();
        booleanTrue = colorAmpersands(cfg.getString("boolean.true", "yes"));
        booleanFalse = colorAmpersands(cfg.getString("boolean.false", "no"));

        DataBase.load(this, cfg.getSection("databases"));
    }

    public BungeeCommandAPI getCommandAPI() {
        return commandAPI;
    }

    public String booleanTrue() {
        return booleanTrue;
    }

    public String booleanFalse() {
        return booleanFalse;
    }

    public String uptime() {
        return getTime((int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime));
    }

    private static String getTime(int seconds) {
        if (seconds < 60) return seconds + "s";
        int minutes = seconds / 60;

        int s = 60 * minutes;

        int secondsLeft = seconds - s;
        if (minutes < 60) {
            if (secondsLeft > 0) return String.valueOf(minutes + "m " + secondsLeft + "s");
            return String.valueOf(minutes + "m");
        }
        StringBuilder time = new StringBuilder();
        if (minutes < 1440) {
            int hours = minutes / 60;

            time.append(hours).append('h');

            int inMins = 60 * hours;

            int leftOver = minutes - inMins;
            if (leftOver >= 1) time.append(' ').append(leftOver).append('m');
            if (secondsLeft > 0) time.append(' ').append(secondsLeft).append('s');
            return time.toString();
        }
        int days = minutes / 1440;

        time.append(days).append('d');

        int inMins = 1440 * days;

        int leftOver = minutes - inMins;
        if (leftOver >= 1) {
            if (leftOver < 60) time.append(' ').append(leftOver).append('m');
            else {
                int hours = leftOver / 60;

                time.append(' ').append(hours).append('h');

                int hoursInMins = 60 * hours;

                int minsLeft = leftOver - hoursInMins;
                if (leftOver >= 1) time.append(' ').append(minsLeft).append('m');
            }
        }
        if (secondsLeft > 0) time.append(' ').append(secondsLeft).append('s');
        return time.toString();
    }

    @Override
    public <T extends Event> Pipeline<T> newPipeline(Class<T> type) {
        return PipelineListener.newPipeline(type, this);
    }

    @Override
    public void onClose() {}

    @Override
    public void post(Runnable task, boolean async) {
        getProxy().getScheduler().runAsync(this, task);
    }
}
