package me.megamichiel.animationlib.config;

import java.io.*;
import java.util.function.Supplier;

public class ConfigFile<C extends ConfigSection> {

    public static <C extends ConfigSection> ConfigFile<C> of(Supplier<C> configSupplier) {
        return new ConfigFile<>(configSupplier);
    }

    public static <C extends ConfigSection> C quickLoad(Supplier<C> configSupplier, File file) throws ConfigException {
        return new ConfigFile<>(configSupplier).file(file).getConfig();
    }

    private final Supplier<C> configSupplier;

    private File configFile;
    private C config;

    public ConfigFile(Supplier<C> configSupplier) {
        this.configSupplier = configSupplier;
    }

    public ConfigFile<C> file(File file) {
        configFile = file;
        return this;
    }

    public File file() {
        return configFile;
    }

    private void checkState() {
        if (configFile == null) {
            throw new IllegalStateException("file(File) has not been called yet!");
        }
    }

    public void saveDefaultConfig(Supplier<InputStream> defaults) throws ConfigException {
        checkState();
        if (!configFile.exists() && (configFile.getParentFile().isDirectory() || configFile.getParentFile().mkdirs())) {
            InputStream in = null;
            OutputStream out = null;
            try {
                if (configFile.createNewFile()) {
                    in = defaults.get();
                    out = new FileOutputStream(configFile);
                    byte[] buf = new byte[4096];
                    for (int read; (read = in.read(buf, 0, 4096)) != -1; ) {
                        out.write(buf, 0, read);
                    }
                }
            } catch (IOException ex) {
                throw new ConfigException("Couldn't write to file", ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public void reloadConfig() throws ConfigException {
        checkState();
        config = configSupplier.get();
        try {
            if (configFile.exists()) {
                config.loadFromFile(configFile);
            }
        } catch (IOException ex) {
            throw new ConfigException("Couldn't open " + configFile.getName(), ex);
        } catch (Exception ex) {
            throw new ConfigException("Couldn't load " + configFile.getName(), ex);
        }
    }

    public C getConfig() throws ConfigException {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    public void saveConfig() throws ConfigException {
        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
