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

    @SuppressWarnings("unchecked")
    public static <T> T[][] split(T[] array, int componentLength) {
        T[][] t = (T[][]) Array.newInstance(array.getClass(), ceilDiv(array.length, componentLength));
        for (int index = 0, i = 0, length = array.length, sublen; i < length; i += componentLength) {
            System.arraycopy(array, i, t[index++] = (T[]) Array.newInstance(array.getClass(), sublen = Math.min(array.length - i, componentLength)), 0, sublen);
        }
        return t;
    }

    public static <T> T[] flip(T[] array) {
        T[] out = Arrays.copyOf(array, array.length);
        for (int i = 0, j = array.length - 1; j >= 0; ) {
            out[i++] = array[j--];
        }
        return out;
    }

    private static int ceilDiv(int val, int div) {
        return (val + div - 1) / div;
    }
}
