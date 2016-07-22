package me.megamichiel.animationlib.config;

import java.io.*;
import java.util.function.Supplier;

public class ConfigManager<C extends AbstractConfig> {

    public static <C extends AbstractConfig> ConfigManager<C> of(Supplier<C> configSupplier) {
        return new ConfigManager<>(configSupplier);
    }

    public static <C extends AbstractConfig> C quickLoad(Supplier<C> configSupplier, File file) {
        return new ConfigManager<>(configSupplier).file(file).getConfig();
    }

    private File configFile;
    private final Supplier<C> configSupplier;
    private C config;

    public ConfigManager(Supplier<C> configSupplier) {
        this.configSupplier = configSupplier;
    }

    public ConfigManager<C> file(File file) {
        configFile = file;
        return this;
    }

    private void checkState() {
        if (configFile == null)
            throw new IllegalStateException("file(File) has not been called yet!");
    }

    public void saveDefaultConfig(Supplier<InputStream> defaults) {
        checkState();
        if (!configFile.exists() && configFile.getParentFile().mkdirs()) {
            try {
                if (configFile.createNewFile()) {
                    InputStream in = defaults.get();
                    OutputStream out = new FileOutputStream(configFile);
                    for (int read; (read = in.read()) != -1;)
                        out.write(read);
                    in.close();
                    out.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        try {
            (config = configSupplier.get()).loadFromFile(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        checkState();
        try {
            (config = configSupplier.get()).loadFromFile(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public C getConfig() {
        if (config == null) reloadConfig();
        return config;
    }

    public void saveConfig() {
        checkState();
        try {
            getConfig().save(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
