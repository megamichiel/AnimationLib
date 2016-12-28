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

    private final Supplier<C> configSupplier;

    private File configFile;
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
        if (!configFile.exists() && (configFile.getParentFile().isDirectory()
                || configFile.getParentFile().mkdirs())) {
            InputStream in = null;
            OutputStream out = null;
            try {
                if (configFile.createNewFile()) {
                    in = defaults.get();
                    out = new FileOutputStream(configFile);
                    byte[] buf = new byte[4096];
                    for (int read; (read = in.read(buf, 0, 4096)) != -1;)
                        out.write(buf, 0, read);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                if (in != null) try {
                    in.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                if (out != null) try {
                    out.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void reloadConfig() {
        checkState();
        try {
            if (configFile.exists())
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
