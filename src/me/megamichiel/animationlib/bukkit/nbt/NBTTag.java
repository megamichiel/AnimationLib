package me.megamichiel.animationlib.bukkit.nbt;

import java.util.Map;

public class NBTTag implements Cloneable {

    private final NBTUtil util;
    private final Object handle;
    private final Map<String, Object> map;

    public NBTTag() {
        map = (util = NBTUtil.getInstance()).getMap(handle = util.createTag());
    }

    public NBTTag(Object handle) {
        util = NBTUtil.getInstance();
        this.handle = handle;
        map = util.getMap(handle);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public void setRaw(String key, Object value) {
        if (value == null) map.remove(key);
        else map.put(key, value);
    }

    public Object getRaw(String key) {
        return map.get(key);
    }

    public void set(String key, Object value) {
        if (value == null) map.remove(key);
        else map.put(key, util.wrap(value));
    }

    public <T> void set(NBTKey<T> key, T value) {
        if (value == null) map.remove(key.id);
        else map.put(key.id, key.modifier.wrap(value));
    }

    public <T> T get(NBTKey<T> key) {
        Object value = map.get(key.id);
        return value != null ? key.modifier.unwrap(value) : null;
    }

    public void remove(String key) {
        map.remove(key);
    }

    public NBTTag createTag(String key) {
        NBTTag tag = new NBTTag();
        map.put(key, tag.getHandle());
        return tag;
    }

    public NBTTag getOrCreateTag(String key) {
        NBTTag tag = getTag(key);
        return tag == null ? createTag(key) : tag;
    }

    public NBTList createList(String key) {
        NBTList list = new NBTList();
        map.put(key, list.getHandle());
        return list;
    }

    public NBTList getOrCreateList(String key) {
        NBTList list = getList(key);
        return list == null ? createList(key) : list;
    }

    public boolean contains(String key) {
        return map.containsKey(key);
    }

    public Object get(String key) {
        Object o = map.get(key);
        return o == null ? null : util.resolve(o.getClass()).unwrap(o);
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String def) {
        Object o = get(key);
        return o instanceof String || isPrimitive(o) ? o.toString() : def;
    }

    public Number getNumber(String key) {
        return getNumber(key, null);
    }

    public Number getNumber(String key, Number def) {
        Object o = get(key);
        return o instanceof Number ? (Number) o : def;
    }

    public boolean getBoolean(String key) {
        return getNumber(key, 0).byteValue() != 0;
    }

    public boolean getBoolean(String key, boolean def) {
        return getNumber(key, def ? 1 : 0).byteValue() != 0;
    }

    public byte getByte(String key) {
        return getNumber(key, (byte) 0).byteValue();
    }

    public byte getByte(String key, byte def) {
        return getNumber(key, def).byteValue();
    }

    public short getShort(String key) {
        return getNumber(key, (short) 0).shortValue();
    }

    public short getShort(String key, short def) {
        return getNumber(key, def).shortValue();
    }

    public int getInt(String key) {
        return getNumber(key, 0).intValue();
    }

    public int getInt(String key, int def) {
        return getNumber(key, def).intValue();
    }

    public long getLong(String key) {
        return getNumber(key, 0L).longValue();
    }

    public long getLong(String key, long def) {
        return getNumber(key, def).longValue();
    }

    public float getFloat(String key) {
        return getNumber(key, 0f).floatValue();
    }

    public float getFloat(String key, float def) {
        return getNumber(key, def).floatValue();
    }

    public double getDouble(String key) {
        return getNumber(key, 0d).doubleValue();
    }

    public double getDouble(String key, double def) {
        return getNumber(key, def).doubleValue();
    }

    public byte[] getByteArray(String key) {
        Object o = get(key);
        return o instanceof byte[] ? (byte[]) o : null;
    }

    public int[] getIntArray(String key) {
        Object o = get(key);
        return o instanceof int[] ? (int[]) o : null;
    }

    public NBTTag getTag(String key) {
        Object o = map.get(key);
        if (o == null) return null;
        try {
            return new NBTTag(o);
        } catch (IllegalStateException ex) { // No NBTTagCompound
            return null;
        }
    }

    public NBTList getList(String key) {
        Object o = map.get(key);
        if (o == null) return null;
        try {
            return new NBTList(o);
        } catch (IllegalStateException ex) { // No NBTTagList
            return null;
        }
    }

    public Object getHandle() {
        return handle;
    }

    @Override
    public NBTTag clone() {
        try {
            return (NBTTag) super.clone();
        } catch (CloneNotSupportedException ex) {
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof NBTTag && ((NBTTag) obj).handle.equals(handle));
    }

    private static boolean isPrimitive(Object o) {
        return o instanceof Number || o instanceof Character || o instanceof Boolean;
    }
}
