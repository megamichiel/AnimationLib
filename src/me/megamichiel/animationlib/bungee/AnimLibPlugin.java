package me.megamichiel.animationlib.bungee;

import me.megamichiel.animationlib.AnimLib;
import me.megamichiel.animationlib.config.ConfigManager;
import me.megamichiel.animationlib.config.YamlConfig;
import me.megamichiel.animationlib.placeholder.StringBundle;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AnimLibPlugin extends Plugin implements AnimLib {

    private static AnimLibPlugin instance;

    private String booleanTrue, booleanFalse;
    private final long startTime = System.currentTimeMillis();

    @Override
    public void onLoad() {
        instance = this;
        StringBundle.setAdapter(BungeePlaceholder::resolve);
    }

    @Override
    public void onEnable() {
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
        booleanTrue = cfg.getString("boolean.true", "yes");
        booleanFalse = cfg.getString("boolean.false", "no");
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

    public static AnimLibPlugin inst() {
        return instance;
    }

    public static String getTime(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        int minutes = seconds / 60;

        int s = 60 * minutes;

        int secondsLeft = seconds - s;
        if (minutes < 60)
        {
            if (secondsLeft > 0) {
                return String.valueOf(minutes + "m " + secondsLeft + "s");
            }
            return String.valueOf(minutes + "m");
        }
        if (minutes < 1440)
        {
            String time = "";

            int hours = minutes / 60;

            time = hours + "h";

            int inMins = 60 * hours;

            int leftOver = minutes - inMins;
            if (leftOver >= 1) {
                time = time + " " + leftOver + "m";
            }
            if (secondsLeft > 0) {
                time = time + " " + secondsLeft + "s";
            }
            return time;
        }
        String time = "";

        int days = minutes / 1440;

        time = days + "d";

        int inMins = 1440 * days;

        int leftOver = minutes - inMins;
        if (leftOver >= 1) {
            if (leftOver < 60)
            {
                time = time + " " + leftOver + "m";
            }
            else
            {
                int hours = leftOver / 60;

                time = time + " " + hours + "h";

                int hoursInMins = 60 * hours;

                int minsLeft = leftOver - hoursInMins;
                if (leftOver >= 1) {
                    time = time + " " + minsLeft + "m";
                }
            }
        }
        if (secondsLeft > 0) {
            time = time + " " + secondsLeft + "s";
        }
        return time;
    }
}
