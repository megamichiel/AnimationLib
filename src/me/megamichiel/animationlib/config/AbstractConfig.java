package me.megamichiel.animationlib.config;

import me.megamichiel.animationlib.config.serialize.ConfigTypeSerializer;
import me.megamichiel.animationlib.config.serialize.ConfigurationSerializationException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public abstract class AbstractConfig {

    public void setIndent(int indent) {
        if (indent < 2 || indent % 2 != 0)
            throw new IllegalArgumentException("Indent must be a multiple newPipeline 2!");
    }

    public abstract String getOriginalKey(String key);

    public void restoreKeys(boolean deep) {
        Set<String> keys = keys();
        for (String key : keys) {
            String s = getOriginalKey(key);
            Object o = get(key);
            if (!s.equals(key)) {
                set(key, null);
                set(s, o);
            }
            if (deep && o instanceof AbstractConfig)
                ((AbstractConfig) o).restoreKeys(true);
        }
    }

    public abstract void set(String path, Object value);

    public abstract void setAll(AbstractConfig config);

    public abstract void setAll(Map<?, ?> map);

    public abstract Object get(String path);

    public Object get(String path, Object def) {
        Object o = get(path);
        return o == null ? def : o;
    }

    public boolean isSet(String path) {
        return get(path) != null;
    }

    public <T> T get(String path, Function<Object, T> func, T def) {
        return Optional.ofNullable(get(path))
                .map(o -> o == null ? def : func.apply(o)).orElse(def);
    }

    public String getString(String path, String def) {
        return get(path, Object::toString, def);
    }

    public String getString(String path) {
        return getString(path, null);
    }

    public boolean isString(String path) {
        Object o = get(path);
        return o instanceof String || isPrimitiveWrapper(o);
    }

    public <E extends Enum<E>> E getEnum(String path, Class<E> type, E def) {
        String s = getString(path);
        if (s == null) return def;
        try {
            return Enum.valueOf(type, s.toLowerCase(Locale.ENGLISH).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }

    public <E extends Enum<E>> E getEnum(String path, Class<E> type) {
        return getEnum(path, type, null);
    }

    public int getInt(String path) {
        return getInt(path, 0);
    }

    public int getInt(String path, int def) {
        Object o = get(path, def);
        return o instanceof Number ? ((Number) o).intValue() : 0;
    }

    public boolean isInt(String path) {
        return get(path) instanceof Number;
    }

    public long getLong(String path, long def) {
        Object o = get(path, def);
        return o instanceof Number ? ((Number) o).longValue() : 0;
    }

    public double getDouble(String path, double def) {
        Object o = get(path, def);
        return o instanceof Number ? ((Number) o).doubleValue() : 0;
    }

    public boolean getBoolean(String path, boolean def) {
        Object o = get(path, def);
        return o instanceof Boolean && (Boolean) o;
    }

    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    public boolean isBoolean(String path) {
        return get(path, null) instanceof Boolean;
    }

    public boolean isSection(String path) {
        return get(path) instanceof AbstractConfig;
    }

    public AbstractConfig getSection(String path) {
        Object o = get(path, null);
        return o instanceof AbstractConfig ? (AbstractConfig) o : null;
    }

    public boolean isList(String path) {
        return get(path) instanceof List;
    }

    public List getList(String path) {
        return get(path, silentCast(List.class), null);
    }

    public <T> List<T> getList(String path, Function<Object, T> func) {
        List<?> list = getList(path);
        List<T> res = new ArrayList<>();
        if (list != null)
            list.stream().map(func::apply).filter(Objects::nonNull).forEach(res::add);
        return res;
    }

    public List<String> getStringList(String path) {
        return getList(path, o -> o instanceof String || isPrimitiveWrapper(o) ? o.toString() : null);
    }

    public List<? extends AbstractConfig> getSectionList(String path) {
        return getList(path, o -> o instanceof AbstractConfig ? (AbstractConfig) o : null);
    }

    public abstract Set<String> keys();

    public abstract Map<String, Object> values();

    public abstract Set<String> deepKeys();

    public abstract Map<String, Object> deepValues();

    public abstract Map<String, Object> toRawMap();

    public abstract AbstractConfig loadFromFile(File file) throws IOException;

    public abstract void save(File file) throws IOException;

    public <T> T loadAsClass(Class<T> clazz) throws ConfigurationSerializationException {
        return new ConfigTypeSerializer(this).loadAsClass(clazz);
    }

    public <T> T loadAsClass(Class<T> clazz,
                             ConfigTypeSerializer.DeserializationContext... ctx)
        throws ConfigurationSerializationException {
        return new ConfigTypeSerializer(this).addDeserializationContext(ctx).loadAsClass(clazz);
    }

    public void saveObject(Object o) {
        new ConfigTypeSerializer(this).saveObject(o);
    }

    private static boolean isPrimitiveWrapper(Object o) {
        return o instanceof Number || o instanceof Boolean;
    }

    public static <I, O> Function<I, O> silentCast(Class<O> target) {
        return i -> target.isInstance(i) ? target.cast(i) : null;
    }
}
