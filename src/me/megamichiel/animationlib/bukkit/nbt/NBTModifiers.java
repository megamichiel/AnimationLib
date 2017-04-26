package me.megamichiel.animationlib.bukkit.nbt;

import java.util.List;
import java.util.Map;

public final class NBTModifiers {

    public static final NBTUtil.Modifier<Byte> BYTE;
    public static final NBTUtil.Modifier<Short> SHORT;
    public static final NBTUtil.Modifier<Integer> INT;
    public static final NBTUtil.Modifier<Long> LONG;
    public static final NBTUtil.Modifier<Float> FLOAT;
    public static final NBTUtil.Modifier<Double> DOUBLE;
    public static final NBTUtil.Modifier<byte[]> BYTE_ARRAY;
    public static final NBTUtil.Modifier<String> STRING;
    public static final NBTUtil.Modifier<List> LIST;
    public static final NBTUtil.Modifier<Map> COMPOUND;
    public static final NBTUtil.Modifier<int[]> INT_ARRAY;

    static {
        NBTUtil nbt = NBTUtil.getInstance();

        BYTE = nbt.modifier(Byte.class);
        SHORT = nbt.modifier(Short.class);
        INT = nbt.modifier(Integer.class);
        LONG = nbt.modifier(Long.class);
        FLOAT = nbt.modifier(Float.class);
        DOUBLE = nbt.modifier(Double.class);
        BYTE_ARRAY = nbt.modifier(byte[].class);
        STRING = nbt.modifier(String.class);
        LIST = nbt.modifier(List.class);
        COMPOUND = nbt.modifier(Map.class);
        INT_ARRAY = nbt.modifier(int[].class);
    }

    private NBTModifiers() {
        throw new AssertionError("This class should not be instantiated");
    }
}
