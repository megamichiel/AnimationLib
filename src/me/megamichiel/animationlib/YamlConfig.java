package me.megamichiel.animationlib;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * A subclass of {@link YamlConfiguration} which:
 * <ul>
 *     <li>Replaces Maps in Lists with ConfigurationSections.</li>
 *     <li>Puts all keys to lower case when loading, to allow for case-insensitive keys</li>
 * </ul>
 * <i>Since: 1.0.0</i>
 *
 * @see YamlConfig#loadConfig(File)
 * @see YamlConfig#loadConfig(Reader)
 */
public class YamlConfig extends YamlConfiguration {

    public YamlConfig() {}

    public YamlConfig(Map<?, ?> map) {
        convertMapsToSections(map, this);
    }

    @Override
    protected void convertMapsToSections(Map<?, ?> input, ConfigurationSection section) {
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            String key = entry.getKey().toString().toLowerCase(Locale.US);
            Object value = entry.getValue();

            if (value instanceof Map) {
                convertMapsToSections((Map<?, ?>) value, section.createSection(key));
            } else if (value instanceof List) {
                section.set(key, convertMapsToSections((List<?>) value));
            } else {
                section.set(key, value);
            }
        }
    }

    protected List<Object> convertMapsToSections(List<?> input) {
        List<Object> list = new ArrayList<>();
        for (Object o : input) {
            if (o instanceof Map) {
                YamlConfig config = new YamlConfig((Map<?, ?>) o);
                list.add(config);
            } else if (o instanceof List) {
                list.add(convertMapsToSections(list));
            } else list.add(o);
        }
        return list;
    }

    public List<ConfigurationSection> getConfigurationList(String path) {
        return getConfigurationList(this, path);
    }

    public static List<ConfigurationSection> getConfigurationList(ConfigurationSection section, String path) {
        List<?> list = section.getList(path);
        if (list == null) return new ArrayList<>();
        List<ConfigurationSection> out = new ArrayList<>();
        for (Object o : list)
            if (o instanceof ConfigurationSection)
                out.add((ConfigurationSection) o);
        return out;
    }

    public static YamlConfig loadConfig(File file) {
        Validate.notNull(file, "File cannot be null");
        YamlConfig config = new YamlConfig();

        try {
            config.load(file);
        } catch (FileNotFoundException ex) {
            Bukkit.getLogger().log(Level.WARNING, "File " + file.getName() + " not found", ex);
        } catch (IOException | InvalidConfigurationException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot load " + file, ex);
        }

        return config;
    }

    public static YamlConfig loadConfig(Reader reader) {
        Validate.notNull(reader, "Stream cannot be null");
        YamlConfig config = new YamlConfig();

        try {
            config.load(reader);
        } catch (IOException | InvalidConfigurationException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot load configuration from stream", ex);
        }
        return config;
    }
}
