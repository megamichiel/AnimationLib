package me.megamichiel.animationlib.bukkit.nbt;

import me.megamichiel.animationlib.util.collect.ConcurrentArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Field handle, tag, map, list, type, unhandledTags;
    private final Constructor<?> tagConstructor, listConstructor;
    private final Map<Class<?>, Modifier<?>> modifiers = new HashMap<>(),
                                             resolved  = new HashMap<>();
    private final String nbtPath;

    private final Method getTypeId, parse, applyToItem;

    {
        Class<? extends ItemStack> itemClass;
        Constructor<? extends ItemStack> itemConstructor;
        Field handle, tag, map, list, type, unhandledTags;
        Constructor<?> tagConstructor, listConstructor;
        String nbtPath;
        Method getTypeId, parse, applyToItem;
        try {
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);

            itemClass = Class.forName(Bukkit.getServer().getClass().getPackage().getName()
                            + ".inventory.CraftItemStack").asSubclass(ItemStack.class);
            (itemConstructor = itemClass.getDeclaredConstructor(ItemStack.class)).setAccessible(true);
            (handle = itemClass.getDeclaredField("handle")).setAccessible(true);
            (tag = handle.getType().getDeclaredField("tag")).setAccessible(true);
            Class<?> tagClass = tag.getType();
            (map = tagClass.getDeclaredField("map")).setAccessible(true);
            modifiers.set(map, map.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            tagConstructor = tagClass.getConstructor();
            nbtPath = tagClass.getName().replace("Compound", "");
            Class<?> listClass = Class.forName(tagClass.getName().replace("Compound", "List"));
            listConstructor = listClass.getConstructor();
            (list = listClass.getDeclaredField("list")).setAccessible(true);
            (type = listClass.getDeclaredField("type")).setAccessible(true);
            getTypeId = Class.forName(tagClass.getName().replace("TagCompound", "Base")).getDeclaredMethod("getTypeId");
            /* Field is not final, but to stay future-proof */
            modifiers.set(list, list.getModifiers() & ~java.lang.reflect.Modifier.FINAL);


            (unhandledTags = Class.forName(itemClass.getPackage().getName() + ".CraftMetaItem")
                    .getDeclaredField("unhandledTags")).setAccessible(true);

            parse = Class.forName(listClass.getPackage().getName() + ".MojangsonParser").getDeclaredMethod("parse", String.class);

            (applyToItem = Class.forName(itemClass.getPackage().getName() + ".CraftMetaItem")
                    .getDeclaredMethod("applyToItem", tagClass)).setAccessible(true);
        } catch (Exception ex) {
            System.err.println("[AnimationLib] Couldn't find itemstack handle, no nbt support ;c: " + ex.getMessage());
            itemClass = null;
            tagConstructor = listConstructor = itemConstructor = null;
            handle = tag = map = list = type = unhandledTags = null;
            nbtPath = null;
            getTypeId = parse = applyToItem = null;
        }
        this.itemClass = itemClass;
        this.itemConstructor = itemConstructor;
        this.handle = handle;
        this.tag = tag;
        this.map = map;
        this.list = list;
        this.type = type;
        this.unhandledTags = unhandledTags;
        this.tagConstructor = tagConstructor;
        this.listConstructor = listConstructor;
        this.nbtPath = nbtPath;

        this.getTypeId = getTypeId;
        this.parse = parse;
        this.applyToItem = applyToItem;

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
                Object obj = handle.get(stack);
                if (obj != null) {
                    this.tag.set(obj, tag);
                }
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return stack;
    }

    public Object toTag(ItemMeta meta) throws IllegalStateException {
        if (meta == null) {
            return null;
        }
        try {
            Object tag = tagConstructor.newInstance(emptyArray);
            applyToItem.invoke(meta, tag);
            return tag;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Object parse(String text) {
        try {
            return parse.invoke(null, text);
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

    public Object createConcurrentTag() {
        try {
            Object tag = tagConstructor.newInstance(emptyArray);
            map.set(tag, new ConcurrentHashMap<>());
            return tag;
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

    public Object createList(byte type) {
        try {
            Object list = listConstructor.newInstance(emptyArray);
            this.type.setByte(list, type);
            return list;
        } catch (Exception ex) {
            return null;
        }
    }

    public void setListType(Object list, byte type) throws IllegalStateException {
        try {
            this.type.setByte(list, type);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void identifyListType(Object list) throws IllegalStateException {
        try {
            List<Object> handle = (List<Object>) this.list.get(list);
            type.setByte(list, handle.isEmpty() ? 0 : (byte) getTypeId.invoke(handle.get(0)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public byte getTypeId(Object nbt) throws IllegalStateException {
        try {
            return (byte) getTypeId.invoke(nbt);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public boolean isSupported(ItemStack item) {
        return itemClass != null && itemClass.isInstance(item);
    }

    public Object getTag(ItemStack stack) throws IllegalStateException {
        try {
            return tag.get(handle.get(stack));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Object getOrCreateTag(ItemStack stack) throws IllegalStateException {
        try {
            Object handle = this.handle.get(stack),
                      tag = this.tag.get(handle);
            if (tag == null) {
                this.tag.set(handle, tag = tagConstructor.newInstance(emptyArray));
            }
            return tag;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Map<String, Object> getMap(Object tag) throws IllegalStateException {
        try {
            return (Map<String, Object>) map.get(tag);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Map<String, Object> getMap(ItemMeta meta) throws IllegalStateException {
        try {
            return (Map<String, Object>) unhandledTags.get(meta);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Map<String, Object> getConcurrentMap(Object tag) throws IllegalStateException {
        try {
            Map<String, Object> map = (Map<String, Object>) this.map.get(tag);
            if (map.getClass() != ConcurrentHashMap.class) {
                this.map.set(tag, map = new ConcurrentHashMap<>(map));
            }
            return map;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public List<Object> getList(Object list) throws IllegalStateException {
        try {
            return (List<Object>) this.list.get(list);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public List<Object> getConcurrentList(Object list) throws IllegalStateException {
        try {
            List<Object> value = (List<Object>) this.list.get(list);
            if (value.getClass() != ConcurrentArrayList.class) {
                this.list.set(list, value = new ConcurrentArrayList<>(value));
            }
            return value;
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Map<String, Object> getOrCreateTagMap(ItemStack stack) throws IllegalStateException {
        try {
            Object handle = this.handle.get(stack);
            Object tag = this.tag.get(handle);
            if (tag == null) this.tag.set(handle, tag = tagConstructor.newInstance(emptyArray));
            return (Map<String, Object>) map.get(tag);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Map<String, Object> getTagMap(ItemStack stack) throws IllegalStateException {
        try {
            return (Map<String, Object>) map.get(tag.get(handle.get(stack)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Map<String, Object> getConcurrentTagMap(ItemStack stack) throws IllegalStateException {
        try {
            Object tag = this.tag.get(handle.get(stack));
            Map<String, Object> map = (Map<String, Object>) this.map.get(tag);
            if (map.getClass() != ConcurrentHashMap.class) {
                this.map.set(tag, map = new ConcurrentHashMap<>(map));
            }
            return map;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Object cloneTag(Object tag) throws IllegalStateException {
        try {
            Object clone = tagConstructor.newInstance(emptyArray);
            ((Map) map.get(clone)).putAll((Map) map.get(tag));
            return clone;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
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

    public <T> Modifier<T> modifier(Class<T> type) throws NoSuchElementException, IllegalArgumentException {
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
                    ((Map) map.get(tag)).putAll((Map) value);
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
                        ((List) list.get(tag)).addAll((Collection) value);
                    } else {
                        List list = (List) this.list.get(tag);
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
            throw new IllegalArgumentException("No corresponding NBT class found for " + type.getName(), ex);
        }
    }

    public <T> Object wrap(T t) throws NoSuchElementException, IllegalStateException {
        return ((Modifier<T>) modifier(t.getClass())).wrap(t);
    }

    public <T> NBTKey<T> createKey(String id, Class<T> type) {
        return new NBTKey<>(id, modifier(type));
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

        public NBTKey<T> createKey(String id) {
            return new NBTKey<>(id, this);
        }
    }
}
