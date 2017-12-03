package me.megamichiel.animationlib.bungee.category;

import me.megamichiel.animationlib.bungee.AnimLibPlugin;
import me.megamichiel.animationlib.bungee.RegisteredPlaceholder;
import me.megamichiel.animationlib.config.ConfigSection;
import me.megamichiel.animationlib.placeholder.Formula;
import me.megamichiel.animationlib.placeholder.ctx.ParsingContext;
import me.megamichiel.animationlib.util.db.SQLHandler;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AnimLibCategory extends PlaceholderCategory {

    private final AnimLibPlugin plugin;
    private final SQLHandler sql;

    private final Map<String, RegisteredPlaceholder> formulas = new HashMap<>();

    protected AnimLibCategory(AnimLibPlugin plugin) {
        super("animlib");
        this.plugin = plugin;
        sql = new SQLHandler(plugin);

        plugin.newPipeline(PostLoginEvent.class)
                .map(PostLoginEvent::getPlayer).forEach(sql::playerJoin);
        plugin.newPipeline(PlayerDisconnectEvent.class)
                .map(PlayerDisconnectEvent::getPlayer).forEach(sql::playerJoin);
        long delay = sql.getRefreshDelay();
        plugin.getProxy().getScheduler().schedule(plugin, sql, delay, delay, TimeUnit.SECONDS);
    }

    public void loadConfig(ConfigSection cfg) {
        formulas.clear();

        String locale = cfg.getString("formula-locale");
        if (locale != null) Formula.setLocale(new Locale(locale));
        if (cfg.isSection("formulas")) {
            ConfigSection section = cfg.getSection("formulas");
            section.forEach((key, value) -> {
                Formula formula;
                if (value instanceof ConfigSection) {
                    ConfigSection sec = (ConfigSection) value;
                    String val = sec.getString("value"),
                            format = sec.getString("format");
                    if (val == null) return;
                    DecimalFormat nf;
                    try {
                        nf = new DecimalFormat(format, Formula.getSymbols());
                    } catch (IllegalArgumentException ex) {
                        plugin.nag("Invalid formula format: " + format);
                        return;
                    }
                    try {
                        formula = Formula.parse(val, ParsingContext.ofFormat(nf));
                    } catch (IllegalArgumentException ex) {
                        plugin.nag("Failed to parse formula " + val + ": " + ex.getMessage());
                        return;
                    }
                } else if (!(value instanceof Collection)) {
                    try {
                        formula = Formula.parse(value.toString(), null);
                    } catch (IllegalArgumentException ex) {
                        plugin.nag("Failed to parse formula " + value + ": " + ex.getMessage());
                        return;
                    }
                } else return;
                formulas.put(section.getOriginalKey(key), formula::invoke);
            });
        }
        sql.load(cfg.getSection("sql-queries"));
    }

    @Override
    public RegisteredPlaceholder get(String value) {
        if (value.startsWith("formula_"))
            return formulas.get(value.substring(8));
        return null;
    }
}
