package me.megamichiel.animationlib.bukkit.nbt;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"WeakerAccess", "unused", "unchecked"})
public class NBTUtil {

    private static final NBTUtil instance = new NBTUtil();

    public static NBTUtil getInstance() {
        return instance;
    }

    public final Object TRUE, FALSE;
    private final Object[] emptyArray = new Object[0];

    private final Class<? extends ItemStack> itemClass;
    private final Constructor<? extends ItemStack> itemConstructor;
    private final Field handleField, tagField, mapField, listField;
    private final Constructor<?> tagConstructor, listConstructor;
    private final Map<Class<?>, Modifier<?>> modifiers = new HashMap<>(),
                                             resolved  = new HashMap<>();
    private final String nbtPath;

    private final Method parseMethod;

    {
        Class<? extends ItemStack> itemClass;
        Constructor<? extends ItemStack> itemConstructor;
        Field handleField, tagField, mapField, listField;
        Constructor<?> tagConstructor, listConstructor;
        String nbtPath;
        Method parseMethod;
        try {
            itemClass = Class.forName(Bukkit.getServer().getClass().getPackage().getName()
                            + ".inventory.CraftItemStack").asSubclass(ItemStack.class);
            (itemConstructor = itemClass.getDeclaredConstructor(ItemStack.class)).setAccessible(true);
            (handleField = itemClass.getDeclaredField("handle")).setAccessible(true);
            (tagField = handleField.getType().getDeclaredField("tag")).setAccessible(true);
            (mapField = tagField.getType().getDeclaredField("map")).setAccessible(true);
            tagConstructor = tagField.getType().getConstructor();
            nbtPath = tagField.getType().getName().replace("Compound", "");
            Class<?> list = Class.forName(tagField.getType().getName().replace("Compound", "List"));
            listConstructor = list.getConstructor();
            (listField = list.getDeclaredField("list")).setAccessible(true);

            parseMethod = Class.forName(list.getPackage().getName() + ".MojangsonParser").getDeclaredMethod("parse", String.class);
        } catch (Exception ex) {
            System.err.println("[AnimatedMenu] Couldn't find itemstack handle, no nbt support ;c");
            itemClass = null;
            tagConstructor = listConstructor = itemConstructor = null;
            handleField = tagField = mapField = listField = null;
            nbtPath = null;
            parseMethod = null;
        }
        this.itemClass = itemClass;
        this.itemConstructor = itemConstructor;
        this.handleField = handleField;
        this.tagField = tagField;
        this.mapField = mapField;
        this.listField = listField;
        this.tagConstructor = tagConstructor;
        this.listConstructor = listConstructor;
        this.nbtPath = nbtPath;

        this.parseMethod = parseMethod;

        Modifier<Boolean> mod = modifier(Boolean.class);
        TRUE = mod.wrap(true);
        FALSE = mod.wrap(false);
    }

    public ItemStack createItemStack(Material type) {
        return asNMS(new ItemStack(type));
    }

    public ItemStack asNMS(ItemStack base) {
        if (itemConstructor != null && !itemClass.isInstance(base)) {
            try {
                return itemConstructor.newInstance(base);
            } catch (Exception ex) {
                // Don't mind
            }
        }
        return base;
    }

    public ItemStack setTag(ItemStack stack, Object tag) throws IllegalStateException {
        if (isSupported(stack = asNMS(stack))) {
            try {
                tagField.set(handleField.get(stack), tag);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return stack;
    }

    public Object parse(String text) {
        try {
            return parseMethod.invoke(null, text);
        } catch (Exception ex) {
            return null;
        }
    }

    public Object createTag() {
        try {
            return tagConstructor.newInstance(emptyArray);
        } catch (Exception ex) {
            return null;
        }
    }

    public Object createList() {
        try {
            return listConstructor.newInstance(emptyArray);
        } catch (Exception ex) {
            return null;
        }
    }

    public boolean isSupported(ItemStack item) {
        return itemClass != null && itemClass.isInstance(item);
    }

    public Object getOrCreateTag(ItemStack stack) throws IllegalStateException {
        try {
            Object handle = handleField.get(stack);
            Object tag = tagField.get(handle);
            if (tag == null) tagField.set(handle, tag = tagConstructor.newInstance(emptyArray));
            return tag;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Map<String, Object> getMap(Object tag) throws IllegalStateException {
        try {
            return (Map<String, Object>) mapField.get(tag);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public List<Object> getList(Object list) throws IllegalStateException {
        try {
            return (List<Object>) listField.get(list);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Map<String, Object> getOrCreateTagMap(ItemStack stack) throws IllegalStateException {
        return getMap(getOrCreateTag(stack));
    }

    public Object cloneTag(Object tag) throws IllegalStateException {
        Object clone = createTag();
        getMap(clone).putAll(getMap(tag));
        return clone;
    }

    public Map<String, Object> unwrap(Map<String, Object> map) throws NoSuchElementException, IllegalStateException {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            out.put(entry.getKey(), resolve(value.getClass()).unwrap(value));
        }
        return out;
    }

    public List<Object> unwrap(List<Object> list) throws NoSuchElementException, IllegalStateException {
        return list.stream().map(o -> resolve(o.getClass()).unwrap(o)).collect(Collectors.toList());
    }

    public Modifier<?> resolve(Class<?> tagType) throws IllegalArgumentException,
                                                       NoSuchElementException {
        Modifier<?> modifier = resolved.get(tagType);
        if (modifier != null) return modifier;
        String name = tagType.getSimpleName(), id = name.substring(6); // "NBTTag".length()
        Class<?> search;
        switch (id) {
            case "ByteArray":   search = byte[].class;  break;
            case "IntArray":    search = int[].class;   break;
            case "Int":         search = Integer.class; break;
            case "List":        search = List.class;    break;
            case "Compound":    search = Map.class;     break;
            default:
                try {
                    search = Class.forName("java.lang." + id);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Unknown NBT class: " + name);
                }
                break;
        }
        return modifier(search);
    }

    public <T> Modifier<T> modifier(Class<T> type) 
            throws NoSuchElementException, IllegalArgumentException {
        Modifier<?> mod = modifiers.get(type);
        if (mod != null) return (Modifier<T>) mod;
        Class<?> data = type;
        String id;
        BiConsumer<Object, Object> converter = null;
        Function<Object, Object> unwrapper = type::cast;
        if (data == byte[].class) id = "ByteArray";
        else if (data == int[].class) id = "IntArray";
        else if (data == Integer.class) id = "Int";
        else if (data == Boolean.class) {
            id = "Byte"; // No special boolean classes, bytes are used
            data = Byte.class;
            Modifier<Byte> bytes = modifier(Byte.class);
            converter = (tag, value) -> {
                try {
                    bytes.data.set(tag, (byte) ((Boolean) value ? 1 : 0));
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException(ex);
                }
            };
            unwrapper = o -> type.cast((byte) o != 0);
        } else if (Map.class.isAssignableFrom(data)) {
            id = "Compound";
            data = Map.class;
            converter = (tag, value) -> {
                try {
                    ((Map) mapField.get(tag)).putAll((Map) value);
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException(ex);
                }
            };
            unwrapper = Function.identity();
        } else if (Iterable.class.isAssignableFrom(data)) {
            id = "List";
            data = List.class;
            converter = (tag, value) -> {
                try {
                    if (value instanceof Collection) {
                        ((List) listField.get(tag)).addAll((Collection) value);
                    } else {
                        List list = (List) listField.get(tag);
                        ((Iterable) value).forEach(list::add);
                    }
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException(ex);
                }
            };
            unwrapper = Function.identity();
        } else id = data.getSimpleName();
        try {
            Class<?> nms = Class.forName(nbtPath + id);
            Modifier<T> res = new Modifier<>(nms, data, converter, unwrapper);
            modifiers.put(type, res);
            resolved.put(nms, res);
            return res;
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("No corresponding NBT class found", ex);
        }
    }

    public <T> Object wrap(T t) throws NoSuchElementException, IllegalStateException {
        return ((Modifier<T>) modifier(t.getClass())).wrap(t);
    }

    public class Modifier<T> {

        final Class<?> type;
        final Constructor<?> wrapper;
        final Field data;
        final BiConsumer<Object, Object> converter;
        final Function<Object, Object> unwrapper;

        Modifier(Class<?> nms, Class<?> type, BiConsumer<Object, Object> converter,
                 Function<Object, Object> unwrapper) throws NoSuchElementException {
            this.type = nms;
            this.converter = converter;
            this.unwrapper = unwrapper;

            Class<?> search = type;
            try {
                if (type.getPackage().getName().equals("java.lang")) {
                    search = (Class<?>) type.getDeclaredField("TYPE").get(null);
                }
            } catch (Exception ex) {
                // No primitive wrapper
            }
            try {
                wrapper = converter != null ? nms.getDeclaredConstructor() : nms.getDeclaredConstructor(search);
            } catch (NoSuchMethodException ex) {
                throw new NoSuchElementException("No constructor found in " + nms.getName() + "!");
            }
            wrapper.setAccessible(true);
            for (Field field : nms.getDeclaredFields()) {
                if (field.getType() == search) {
                    field.setAccessible(true);
                    data = field;
                    return;
                }
            }
            throw new NoSuchElementException("No data field found for " + type + "!");
        }

        public List<T> unwrapList(List<?> list) throws IllegalStateException {
            return list.stream().filter(this::isInstance).map(this::unwrap).collect(Collectors.toList());
        }

        public List<?> wrapList(List<T> list) throws IllegalStateException {
            return list.stream().map(this::wrap).collect(Collectors.toList());
        }

        public Object wrap(T value) throws IllegalStateException {
            try {
                if (converter != null) {
                    Object o = wrapper.newInstance(emptyArray);
                    converter.accept(o, value);
                    return o;
                }
                return wrapper.newInstance(value);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        public T unwrap(Object value) throws IllegalStateException {
            try {
                return (T) unwrapper.apply(data.get(value));
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(ex);
            }
        }

        public boolean isInstance(Object value) {
            try {
                return type.isInstance(value);
            } catch (Exception ex) {
                return false;
            }
        }
    }
}
