package me.megamichiel.animationlib.config.serialize;

import me.megamichiel.animationlib.config.ConfigSection;
import me.megamichiel.animationlib.config.MapConfig;
import me.megamichiel.animationlib.util.DynamicSwitch;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiFunction;

import static me.megamichiel.animationlib.util.DynamicSwitch.Case;

public class ConfigTypeSerializer {

    private static final DynamicSwitch<Class> primitiveDefaults = DynamicSwitch.of(
            Case(boolean.class, () -> Boolean.FALSE),
            Case(char.class, (char) 0),
            Case(byte.class, (byte) 0),
            Case(short.class, (short) 0),
            Case(int.class, 0),
            Case(long.class, 0L),
            Case(float.class, 0F),
            Case(double.class, 0D)
    );

    public static final DeserializationContext EMPTY_ARRAYS = field -> {
        return field.getType().isArray() ? Array.newInstance(
                field.getType().getComponentType(), 0) : null;
    }, PRIMITIVE_DEFAULTS = field -> {
        Class<?> type = field.getType();
        return type.isPrimitive() ? primitiveDefaults.apply(type) : null;
    }, ENUM_DEFAULTS = field -> {
        Class<?> type = field.getType();
        return type.isEnum() ? type.getEnumConstants()[0] : null;
    };

    public static Class<?> getArrayComponent(Class<?> array) {
        while (array.isArray()) array = array.getComponentType();
        return array;
    }

    private final ConfigSection config;
    private final List<DeserializationContext> deserializationContexts = new ArrayList<>();

    public ConfigTypeSerializer(ConfigSection config) {
        this.config = config;
    }

    private ConfigTypeSerializer(ConfigSection config, ConfigTypeSerializer parent) {
        this.config = config;
        deserializationContexts.addAll(parent.deserializationContexts);
    }

    public ConfigTypeSerializer addDeserializationContext(DeserializationContext... contexts) {
        Collections.addAll(deserializationContexts, contexts);
        return this;
    }

    public <T> T loadAsClass(Class<T> clazz) throws ConfigurationSerializationException {
        return loadAsClass(clazz, "");
    }

    private Object getDefault(Field field) {
        Object result;
        for (DeserializationContext ctx : deserializationContexts) {
            result = ctx.getDefault(field);
            if (result != null) return result;
        }
        return null;
    }

    private <T> T loadAsClass(Class<T> clazz, String path) throws ConfigurationSerializationException {
        T instance;
        try {
            instance = clazz.newInstance();
        } catch (InstantiationException ex) {
            throw new ConfigurationSerializationException(path,
                    ConfigurationSerializationException.Type.NEW_INSTANCE,
                    ex, "Unable to create instance");
        } catch (IllegalAccessException ex) {
            throw new ConfigurationSerializationException(path,
                    ConfigurationSerializationException.Type.NO_CONSTRUCTOR_ACCESS,
                    ex, "No access to class constructor " + clazz.getName());
        }
        if (clazz.getSuperclass() == null) return instance;
        for (Class<?> raw = clazz; raw != Object.class; raw = raw.getSuperclass()) {
            for (Field field : raw.getDeclaredFields()) {
                int mod = field.getModifiers();
                if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod)
                        && !Modifier.isTransient(mod)) { // Skipping deez
                    boolean acc = field.isAccessible();
                    if (!acc) field.setAccessible(true);
                    String configName = getConfigName(field);
                    Object configValue = config.get(configName);
                    String fPath = (path.length() == 0 ? "" : (path + '.'))
                            + configName;
                    Class<?> type = field.getType();
                    if (type.isPrimitive()) {
                        try {
                            Object primitive = getPrimitive(configValue, type, fPath, field);
                            if (primitive == null)
                                throw new ConfigurationSerializationException(fPath,
                                        ConfigurationSerializationException.Type.INVALID_TYPE,
                                        new NullPointerException(),
                                        "Expected " + type.getName() + ", got nothing");
                            DynamicSwitch.<Class>of(
                                    Case(Boolean.class,     () -> field.setBoolean(instance, (boolean) primitive)),
                                    Case(Character.class,   () -> field.setChar(instance, (char) primitive)),
                                    Case(Byte.class,        () -> field.setByte(instance, (byte) primitive)),
                                    Case(Short.class,       () -> field.setShort(instance, (short) primitive)),
                                    Case(Integer.class,     () -> field.setInt(instance, (int) primitive)),
                                    Case(Long.class,        () -> field.setLong(instance, (long) primitive)),
                                    Case(Float.class,       () -> field.setFloat(instance, (float) primitive)),
                                    Case(Double.class,      () -> field.setDouble(instance, (double) primitive))
                            ).apply(primitive.getClass(), ex -> {
                                if (ex instanceof IllegalAccessException)
                                    throw new ConfigurationSerializationException(fPath,
                                            ConfigurationSerializationException.Type.NO_FIELD_ACCESS,
                                            ex, "Cannot set primitive field " + field);
                            });
                        } catch (ClassCastException ex) {
                            throw new ConfigurationSerializationException(fPath,
                                    ConfigurationSerializationException.Type.INVALID_TYPE,
                                    ex, "Expected " + type.getName() + ", got " + className(configValue));
                        }
                    } else if (type.isArray()) {
                        try {
                            Object array = getArray(configValue, type.getComponentType(), fPath);
                            if (array == null) array = getDefault(field);
                            field.set(instance, array);
                        } catch (IllegalAccessException ex) {
                            throw new ConfigurationSerializationException(fPath,
                                    ConfigurationSerializationException.Type.NO_FIELD_ACCESS,
                                    ex, "Cannot set array field " + field);
                        } catch (ClassCastException ex) {
                            throw new ConfigurationSerializationException(fPath,
                                    ConfigurationSerializationException.Type.INVALID_TYPE,
                                    ex, "Not a list");
                        }
                    } else if (type == String.class) {
                        try {
                            String value = config.getString(configName);
                            field.set(instance, value == null ? getDefault(field) : value);
                        } catch (IllegalAccessException ex) {
                            throw new ConfigurationSerializationException(fPath,
                                    ConfigurationSerializationException.Type.NO_FIELD_ACCESS,
                                    ex, "Cannot set String field " + field);
                        }
                    } else if (type.isEnum()) {
                        String string = config.getString(configName);
                        if (string != null) {
                            string = string.toUpperCase(Locale.US).replace('-', '_');
                            try {
                                Object value = Enum.valueOf(
                                        type.asSubclass(Enum.class), string);
                                field.set(instance, value);
                            } catch (IllegalArgumentException ex) {
                                throw new ConfigurationSerializationException(fPath,
                                        ConfigurationSerializationException.Type.INVALID_TYPE,
                                        ex, "Not an enum value of " + type.getName() + ": " + string);
                            } catch (IllegalAccessException ex) {
                                throw new ConfigurationSerializationException(fPath,
                                        ConfigurationSerializationException.Type.NO_FIELD_ACCESS,
                                        ex, "Cannot set Enum field " + field);
                            }
                        } else {
                            Object def = getDefault(field);
                            if (def != null) {
                                 try {
                                     field.set(instance, def);
                                 } catch (IllegalAccessException ex) {
                                     throw new ConfigurationSerializationException(fPath,
                                             ConfigurationSerializationException.Type.NO_FIELD_ACCESS,
                                             ex, "Cannot set Enum field " + field);
                                 }
                            }
                        }
                    } else {
                        Object val = getPrimitive(configValue, type, fPath, field);
                        try {
                            if (val != null) field.set(instance, val);
                        } catch (IllegalAccessException ex) {
                            throw new ConfigurationSerializationException(fPath,
                                    ConfigurationSerializationException.Type.NO_FIELD_ACCESS,
                                    ex, "Cannot set field " + field);
                        } catch (IllegalArgumentException ex) {
                            throw new ConfigurationSerializationException(fPath,
                                    ConfigurationSerializationException.Type.INVALID_TYPE,
                                    ex, "Expected " + type.getName() + ", got " + className(val));
                        }
                    }

                    if (!acc) field.setAccessible(false);
                }
            }
        }
        return instance;
    }

    private Object getPrimitive(Object configValue, Class<?> type,
                                String path, Field field) throws ConfigurationSerializationException {
        if (configValue == null) configValue = getDefault(field);
        if (type == Boolean.TYPE) {
            if (configValue instanceof Boolean) return configValue;
            throw new ConfigurationSerializationException(path,
                    ConfigurationSerializationException.Type.INVALID_TYPE,
                    new ClassCastException(), "Expected a boolean, got " + className(configValue));
        } else if (type == Character.TYPE) {
            if (configValue instanceof Character)
                return configValue;
            if (configValue instanceof Number)
                return (char) ((Number) configValue).intValue();
            if (configValue instanceof String && ((String) configValue).length() == 1)
                return ((String) configValue).charAt(0);
            throw new ConfigurationSerializationException(path,
                    ConfigurationSerializationException.Type.INVALID_TYPE,
                    new ClassCastException(), "Expected a character, got " + className(configValue));
        } else if (configValue instanceof Number) {
            Number num = (Number) configValue;
            return DynamicSwitch.<Class>of(
                    () -> { throw new ConfigurationSerializationException(path,
                            ConfigurationSerializationException.Type.INVALID_TYPE,
                            null, "Unknown config type: " + type.getName()); },
                    Case(byte.class,    num::byteValue),
                    Case(short.class,   num::shortValue),
                    Case(int.class,     num::intValue),
                    Case(long.class,    num::longValue),
                    Case(float.class,   num::floatValue),
                    Case(double.class,  num::doubleValue)
            ).apply(type);
        } else if (configValue instanceof ConfigSection) {
            return new ConfigTypeSerializer((ConfigSection) configValue, this)
                    .loadAsClass(type, path);
        }
        return configValue;
    }

    private Object getArray(Object configValue, Class<?> type /* = componentType */, String path)
            throws ConfigurationSerializationException {
        if (configValue instanceof List<?>) {
            List<?> cfg = (List<?>) configValue;
            BiFunction<Integer, Object, Object> resolver;
            if (type.isArray()) {
                Class<?> component = type.getComponentType();
                resolver = (index, value) -> getArray(value, component, path(path, index));
            } else if (type.isEnum()) {
                resolver = (index, value) -> {
                    String string = value.toString().toUpperCase(Locale.US).replace('-', '_');
                    try {
                        return Enum.valueOf(type.asSubclass(Enum.class),
                                value.toString().toUpperCase(Locale.US).replace('-', '_'));
                    } catch (IllegalArgumentException ex) {
                        throw new ConfigurationSerializationException(path(path, index),
                                ConfigurationSerializationException.Type.INVALID_TYPE,
                                ex, "Not an enum value of " + type.getName() + ": " + string);
                    }
                };
            } else {
                resolver = (index, value) -> {
                    if (value instanceof String || value instanceof Boolean ||
                            value instanceof Character || value instanceof Byte ||
                            value instanceof Short || value instanceof Integer ||
                            value instanceof Long || value instanceof Float ||
                            value instanceof Double) return value;
                    if (value instanceof ConfigSection) {
                        return new ConfigTypeSerializer((ConfigSection) value, this)
                                .loadAsClass(type, path(path, index));
                    }
                    return null;
                };
            }
            int size = cfg.size();
            Object[] array = new Object[size];
            int actualSize = 0;
            for (int i = 0; i < size; i++) {
                Object resolved = resolver.apply(i, cfg.get(i));
                if (resolved != null) array[actualSize++] = resolved;
            }
            Object result = Array.newInstance(type, actualSize);
            int resultSize = 0;
            for (int i = 0; i < actualSize; i++) {
                Object val = array[i];
                if (type.isInstance(val)) Array.set(result, resultSize++, val);
                else if (type.isPrimitive()) {
                    int index = resultSize;
                    Object output = DynamicSwitch.<Class>of(() -> false,
                            Case(boolean.class, () -> Array.setBoolean(result, index, (boolean) val)),
                            Case(char.class,    () -> {
                                if (val instanceof Character) Array.setChar(result, index, (char) val);
                                else if (val instanceof String && ((String) val).length() == 1)
                                    Array.setChar(result, index, ((String) val).charAt(0));
                                else throw new ClassCastException();
                            }),
                            Case(byte.class,    () -> Array.setByte(   result, index, ((Number) val).byteValue())),
                            Case(short.class,   () -> Array.setShort(  result, index, ((Number) val).shortValue())),
                            Case(int.class,     () -> Array.setInt(    result, index, ((Number) val).intValue())),
                            Case(long.class,    () -> Array.setLong(   result, index, ((Number) val).longValue())),
                            Case(float.class,   () -> Array.setFloat(  result, index, ((Number) val).floatValue())),
                            Case(double.class,  () -> Array.setDouble( result, index, ((Number) val).doubleValue()))
                    ).apply(type, err -> {
                        if (err instanceof ClassCastException)
                            throw new ConfigurationSerializationException(path(path, index),
                                    ConfigurationSerializationException.Type.INVALID_TYPE,
                                    err, "Expected " + type.getName()
                                    + ", got " + val.getClass().getName());
                    });
                    if (output == null) resultSize++;
                }
            }
            if (resultSize == actualSize) return result;
            Object out = Array.newInstance(type, resultSize);
            for (int i = 0; i < resultSize; i++)
                Array.set(out, i, Array.get(result, i));
            return out;
        }
        return null;
    }

    private static final String letters = "QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm";

    private String getConfigName(Field field) {
        String name = field.getName();
        StringBuilder sb = new StringBuilder();
        char c;
        for (int i = 0, index, length = name.length(); i < length; i++) {
            index = letters.indexOf(c = name.charAt(i));
            if (index == -1 || index >= 26) sb.append(c);
            else sb.append('-').append(letters.charAt(index + 26));
        }
        return sb.toString();
    }

    private String path(String current, Object add) {
        return current.isEmpty() ? add.toString() : (current + '.' + add);
    }

    private String className(Object o) {
        return o == null ? "nothing" : o.getClass().getName();
    }

    public interface DeserializationContext {
        Object getDefault(Field field);
    }

    public void saveObject(Object o) throws ConfigurationSerializationException {
        saveObject(o, "");
    }

    private void saveObject(Object o, String path) {
        Class<?> c = o.getClass();
        if (c.getSuperclass() == null) return;
        for (; c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                boolean accessible = field.isAccessible();
                if (!accessible) field.setAccessible(true);
                Class<?> type = field.getType();
                String configName = getConfigName(field),
                        fPath = path(path, configName);
                try {
                    Object value = field.get(o);
                    if (value != null) {
                        config.set(configName, serialize(value, type, fPath));
                    }
                } catch (IllegalAccessException ex) {
                    throw new ConfigurationSerializationException(fPath,
                            ConfigurationSerializationException.Type.NO_FIELD_ACCESS,
                            ex, "Cannot get value from field " + field);
                }
                if (!accessible) field.setAccessible(false);
            }
        }
    }

    private static final List<Class<?>> primitiveWrappers = Arrays.asList(
            Boolean.class, Character.class, Byte.class, Short.class,
            Integer.class, Long.class, Float.class, Double.class
    );

    private Object serialize(Object o, Class<?> type, String path) {
        if (type.isArray()) {
            List<Object> result = new ArrayList<>();
            Class<?> component = type.getComponentType();
            Object value;
            for (int i = 0, length = Array.getLength(o); i < length; i++) {
                value = Array.get(o, i);
                if (value != null && (value = serialize(value,
                        component, path(path, i))) != null)
                    result.add(value);
            }
            return result;
        }
        boolean primitive = type.isPrimitive();
        if (primitive || primitiveWrappers.contains(type)) {
            if (!primitive && o == null) return null;
            return DynamicSwitch.<Class>of(
                    Case(int.class, ((Number) o).intValue(),
                            byte.class, short.class, Integer.class, Byte.class, Short.class),
                    Case(long.class, ((Number) o).longValue(), Long.class),
                    Case(float.class, ((Number) o).doubleValue(),
                            double.class, Float.class, Double.class),
                    Case(boolean.class, o, Boolean.class),
                    Case(char.class, o, Character.class)
            ).apply(type);
        }
        if (type == String.class) return o;
        if (type.isEnum()) return ((Enum<?>) o).name().toLowerCase(Locale.US).replace('-', '_');
        MapConfig config = new MapConfig();
        new ConfigTypeSerializer(config, this).saveObject(o, path);
        return config;
    }
}
