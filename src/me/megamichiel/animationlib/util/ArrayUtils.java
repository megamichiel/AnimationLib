package me.megamichiel.animationlib.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ArrayUtils {

    public static <T> List<T> asList(T[] array) {
        switch (array.length) {
            case 0: return Collections.emptyList();
            case 1: return Collections.singletonList(array[0]);
            default: return Arrays.asList(array);
        }
    }

    public static <T> List<T> clean(List<T> list) {
        switch (list.size()) {
            case 0: return Collections.emptyList();
            case 1: return Collections.singletonList(list.get(0));
            default: return list;
        }
    }

    public static <T> T[] subArray(T[] array, int index) {
        return Arrays.copyOfRange(array, index, array.length);
    }

    public static <T> T[][] split(T[] array, int componentLength) {
        int length = ceil(array.length, componentLength);
        T[][] t = (T[][]) Array.newInstance(array.getClass(), length);
        for (int i = 0; i < length; i++)
            t[i] = Arrays.copyOfRange(array, i * componentLength,
                    Math.min(array.length, (i + 1) * componentLength));
        return t;
    }

    public static <T> T[] flip(T[] array) {
        T[] out = Arrays.copyOf(array, array.length);
        for (int i = 0, j = array.length - 1; j >= 0; i++, j--)
            out[i] = array[j];
        return out;
    }

    private static int ceil(int val, int mod) {
        return (int) Math.ceil((double) val / mod) * mod;
    }
}
