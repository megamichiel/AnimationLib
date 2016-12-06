package me.megamichiel.animationlib.bungee;

import me.megamichiel.animationlib.AnimLib;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.config.ConfigManager;
import me.megamichiel.animationlib.config.type.YamlConfig;
import me.megamichiel.animationlib.placeholder.StringBundle;
import me.megamichiel.animationlib.placeholder.Formula;
import me.megamichiel.animationlib.util.LoggerNagger;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static me.megamichiel.animationlib.placeholder.StringBundle.colorAmpersands;

public class AnimLibPlugin extends Plugin implements AnimLib, LoggerNagger {

    private String booleanTrue, booleanFalse;
    private final long startTime = System.currentTimeMillis();

    private final BungeeCommandAPI commandAPI = new BungeeCommandAPI();

    private final Map<String, RegisteredPlaceholder> formulas = new HashMap<>();

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

        formulas.clear();
        String locale = cfg.getString("formula-locale");
        if (locale != null) Formula.setLocale(new Locale(locale));
        if (cfg.isSection("formulas")) {
            AbstractConfig section = cfg.getSection("formulas");
            for (String key : section.keys()) {
                String val = section.getString(key);
                String origin = section.getOriginalKey(key);
                Formula formula;
                if (val != null) {
                    formula = Formula.parse(val, null);
                    if (formula == null) continue;
                    formulas.put(origin, formula::invoke);
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
                        formulas.put(origin, formula::invoke);
                    }
                }
            }
        }
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

    public RegisteredPlaceholder getFormula(String id) {
        return formulas.get(id);
    }

    public static String getTime(int seconds) {
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
}
