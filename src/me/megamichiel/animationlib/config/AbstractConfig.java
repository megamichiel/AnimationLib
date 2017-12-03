package me.megamichiel.animationlib.config;

import me.megamichiel.animationlib.config.serialize.ConfigTypeSerializer;
import me.megamichiel.animationlib.config.serialize.ConfigurationSerializationException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractConfig {

    public abstract void setIndent(int indent);

    public abstract String getOriginalKey(String key);
    
    public abstract void restoreKeys(boolean deep);

    public abstract void set(String path, Object value);

    public abstract void setAll(AbstractConfig config);
    
    public abstract void setAll(ConfigSection section);

    public abstract void setAll(Map<?, ?> map);

    public abstract Object get(String path);

    public abstract Object get(String path, Object def);

    public abstract boolean isSet(String path);

    public abstract void replace(String path, Function<Object, ?> func);

    public abstract <T> T get(String path, Function<Object, T> func, T def);

    public abstract String getString(String path, String def);

    public abstract String getString(String path);

    public abstract boolean isString(String path);

    public abstract <E extends Enum<E>> E getEnum(String path, Class<E> type, E def);

    public abstract <E extends Enum<E>> E getEnum(String path, Class<E> type);

    public abstract int getInt(String path);

    public abstract int getInt(String path, int def);

    public abstract boolean isInt(String path);

    public abstract long getLong(String path);

    public abstract long getLong(String path, long def);

    public abstract double getDouble(String path);

    public abstract double getDouble(String path, double def);

    public abstract boolean getBoolean(String path, boolean def);

    public abstract boolean getBoolean(String path);

    public abstract boolean isBoolean(String path);

    public abstract boolean isSection(String path);

    public abstract AbstractConfig getSection(String path);

    public abstract boolean isList(String path);

    public abstract List getList(String path);

    public abstract <T> List<T> getList(String path, Function<Object, T> func);

    public abstract List<String> getStringList(String path);

    public abstract List<ConfigSection> getSectionList(String path);

    public abstract Set<String> keys();

    public abstract Map<String, Object> values();

    public abstract Set<String> deepKeys();

    public abstract Map<String, Object> deepValues();

    public abstract Map<String, Object> toRawMap();

    public abstract void loadFromFile(File file) throws IOException;

    public abstract void save(File file) throws IOException;

    public abstract void forEach(BiConsumer<String, Object> action);

    public abstract <T> void forEach(BiFunction<String, Object, T> func, BiConsumer<String, T> action);

    public abstract void forEachKey(Consumer<String> action);

    public abstract <T> T loadAsClass(Class<T> clazz) throws ConfigurationSerializationException;

    public abstract <T> T loadAsClass(Class<T> clazz, ConfigTypeSerializer.DeserializationContext... ctx)
        throws ConfigurationSerializationException;

    public abstract void saveObject(Object o);

    public static boolean isPrimitiveWrapper(Object o) {
        return o instanceof Number || o instanceof Boolean || o instanceof Character;
    }

    public static <I, O> Function<I, O> silentCast(Class<O> target) {
        return i -> target.isInstance(i) ? target.cast(i) : null;
    }
}
