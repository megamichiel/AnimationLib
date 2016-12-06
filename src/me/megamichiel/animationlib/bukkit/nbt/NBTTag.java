package me.megamichiel.animationlib.bukkit.nbt;

import java.util.Map;

public class NBTTag implements Cloneable {

    private final NBTUtil util;
    private final Object handle;
    private final Map<String, Object> map;

    public NBTTag() {
        util = NBTUtil.getInstance();
        handle = util.createTag();
        map = util.getMap(handle);
    }

    public NBTTag(Object handle) {
        util = NBTUtil.getInstance();
        this.handle = handle;
        map = util.getMap(handle);
    }

    public void setRaw(String key, Object value) {
        if (value == null) map.remove(key);
        else map.put(key, value);
    }

    public void set(String key, Object value) {
        if (value == null) map.remove(key);
        else map.put(key, util.wrap(value));
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

    public boolean contains(String name) {
        return map.containsKey(name);
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
        return getByte(key) != 0;
    }

    public boolean getBoolean(String key, boolean def) {
        return getNumber(key, def ? 1 : 0).intValue() != 0;
    }

    public byte getByte(String key) {
        return getNumber(key, 0).byteValue();
    }

    public byte getByte(String key, byte def) {
        return getNumber(key, def).byteValue();
    }

    public short getShort(String key) {
        return getNumber(key, 0).shortValue();
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
        return getNumber(key, 0).longValue();
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

    private static boolean isPrimitive(Object o) {
        return o instanceof Number || o instanceof Character || o instanceof Boolean;
    }
}
