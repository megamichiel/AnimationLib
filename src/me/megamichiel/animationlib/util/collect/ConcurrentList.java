package me.megamichiel.animationlib.util.collect;

import java.util.Collection;
import java.util.List;

public interface ConcurrentList<E> extends List<E> {

    boolean addBefore(Object o, E e);

    boolean addAfter(Object o, E e);

    boolean addAllBefore(Object o, Collection<? extends E> c);

    boolean addAllAfter(Object o, Collection<? extends E> c);

    boolean replaceFirst(Object o, E e);

    boolean replaceLast(Object o, E e);

    boolean replaceAll(Object o, E e);
}
