package me.megamichiel.animationlib.util.collect;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unchecked")
public class ConcurrentArrayList<E> implements List<E> {

    public static void main(String[] args) {
        for (List<Integer> list : new List[] { Collections.synchronizedList(new ArrayList<>()), new CopyOnWriteArrayList<>(), new ConcurrentArrayList<>() }) {
            AtomicInteger count = new AtomicInteger(10);
            for (int i = 0, c = count.get(); i++ < c; ) {
                new Thread(() -> {
                    long time = System.currentTimeMillis();
                    for (int j = 0; j < 10000; ) {
                        list.add(j++);
                    }
                    System.out.println("Took: " + (System.currentTimeMillis() - time));
                    count.getAndDecrement();
                }).start();
            }
            //noinspection StatementWithEmptyBody
            while (count.get() != 0);
            Map<Integer, AtomicInteger> counter = new HashMap<>();
            for (Integer i : list) {
                counter.compute(i, (key, value) -> {
                    if (value == null) {
                        return new AtomicInteger(1);
                    }
                    value.getAndIncrement();
                    return value;
                });
            }
            System.out.println(counter);
        }
    }

    private final AtomicInteger _size = new AtomicInteger();
    private final AtomicBoolean lock = new AtomicBoolean();
    private Object[] _elements = new Object[10];

    public ConcurrentArrayList() { }

    public ConcurrentArrayList(Collection<? extends E> c) {
        addAll(c);
    }

    @Override
    public int size() {
        return _size.get();
    }

    @Override
    public boolean isEmpty() {
        return _size.get() == 0;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr<>(this, 0);
    }

    @Override
    public Object[] toArray() {
        acquireLock();

        try {
            int size = _size.get();
            Object[] result = new Object[size];
            System.arraycopy(_elements, 0, result, 0, size);
            return result;
        } finally {
            unlock();
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {
        acquireLock();

        try {
            return _toArray(a, 0, _size.get());
        } finally {
            unlock();
        }
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    private <T> T[] _toArray(T[] a, int from, int size) {
        if (a.length < size) {
            Class<?> type = a.getClass();
            System.arraycopy(_elements, from, a = type == Object[].class ? (T[]) new Object[size] : (T[]) Array.newInstance(type.getComponentType(), size), 0, size);
            return a;
        }
        if (size > 0) {
            System.arraycopy(_elements, from, a, 0, size);
        }
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void acquireLock() {
        while (!lock.compareAndSet(false, true)); // Bare bones lock system,
    }

    private void unlock() {
        lock.set(false);
    }

    private String bounds(int index, int size) {
        return "Index: " + index + ", size: " + size;
    }

    @Override
    public E get(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(bounds(index, _size.get()));
        }
        Object e;
        try {
            e = _elements[index];
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IndexOutOfBoundsException(bounds(index, _size.get()));
        }
        if (e == null) { // A change might be occurring, check inside lock
            int size = _size.get();
            if (index >= size) {
                throw new IndexOutOfBoundsException(bounds(index, size));
            }
            acquireLock();
            try {
                if (index < (size = _size.get())) {
                    return (E) _elements[index];
                }
            } finally {
                unlock();
            }
            throw new IndexOutOfBoundsException(bounds(index, size));
        }
        return (E) e;
    }

    @Override
    public E set(int index, E e) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(bounds(index, _size.get()));
        }
        acquireLock();

        try {
            int size = _size.get();
            if (index >= size) {
                throw new IndexOutOfBoundsException(bounds(index, size));
            }
            Object old = _elements[index];
            _elements[index] = e;

            return (E) old;
        } finally {
            unlock();
        }
    }

    @Override
    public boolean add(E e) {
        acquireLock();

        try {
            int size = _size.get();
            Object[] elements = _elements;

            if (_size.incrementAndGet() == elements.length) {
                System.arraycopy(elements, 0, elements = _elements = new Object[size << 1], 0, size);
            }
            elements[size] = e;

            return true;
        } finally {
            unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        return _remove(o, 0, -1);
    }

    private boolean _remove(Object o, int start, int end) {
        acquireLock();

        try {
            Object[] elements = _elements;
            for (int index = 0, size = _size.get(); index < size; ++index) {
                if (o == null ? elements[index] == null : o.equals(elements[index])) {
                    if (index >= start && (end == -1 || index < end)) {
                        System.arraycopy(elements, index + 1, elements, index, _size.getAndDecrement() - index);
                        return true;
                    }
                    return false;
                }
            }
            return false;
        } finally {
            unlock();
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (indexOf(o) == -1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void add(int index, E element) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(bounds(index, _size.get()));
        }
        acquireLock();
        try {
            int size = _size.get();
            Object[] elements = _elements;
            if (index > size) {
                throw new IndexOutOfBoundsException(bounds(index, size));
            } else {
                if (_size.incrementAndGet() == elements.length) {
                    System.arraycopy(elements, 0, elements = new Object[size << 1], 0, index);
                    System.arraycopy(_elements, index, _elements = elements, index + 1, size - index);
                } else {
                    System.arraycopy(elements, index, elements, index + 1, size - index);
                }
                elements[index] = element;
            }
        } finally {
            unlock();
        }
    }

    @Override
    public E remove(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(bounds(index, _size.get()));
        }
        acquireLock();

        try {
            int size = _size.get();
            if (index > size) {
                throw new IndexOutOfBoundsException(bounds(index, size));
            }
            Object[] elements = _elements;
            Object element = elements[index];
            System.arraycopy(elements, index + 1, elements, index, size - index); // Copy extra null element
            _size.getAndDecrement();
            return (E) element;
        } finally {
            unlock();
        }
    }

    @Override
    public int indexOf(Object o) {
        if (o == null) {
            boolean hasLock = false;

            Object[] elements = _elements;
            try {
                for (int i = 0; ; ) {
                    if (elements[i] == null) {
                        if (!hasLock) {
                            acquireLock();
                            hasLock = true;
                            continue;
                        }
                        return i >= _size.get() ? -1 : i;
                    }
                    ++i;
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                return -1;
            } finally {
                if (hasLock) {
                    unlock();
                }
            }
        } else {
            try {
                for (int i = 0, size = _size.get(); i < size; ++i) {
                    if (o.equals(_elements[i])) {
                        return i;
                    }
                }
                return -1;
            } catch (ArrayIndexOutOfBoundsException ex) {
                return -1;
            }
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        if (o == null) {
            boolean hasLock = false;

            Object[] elements = _elements;
            try {
                for (int i = _size.get() - 1; i >= 0; ) {
                    if (elements[i] == null) {
                        if (!hasLock) {
                            acquireLock();
                            hasLock = true;
                            continue;
                        }
                        return i >= _size.get() ? -1 : i;
                    }
                    --i;
                }
                return -1;
            } catch (ArrayIndexOutOfBoundsException ex) {
                return -1;
            } finally {
                if (hasLock) {
                    unlock();
                }
            }
        } else {
            try {
                for (int i = _size.get() - 1; i >= 0; --i) {
                    if (o.equals(_elements[i])) {
                        return i;
                    }
                }
                return -1;
            } catch (ArrayIndexOutOfBoundsException ex) {
                return -1;
            }
        }
    }

    @Override
    public void clear() {
        acquireLock();
        try {
            Object[] elements = _elements;
            int size = _size.getAndSet(0), index = 1;
            switch (size) {
                case 2:
                    elements[1] = null;
                case 1:
                    elements[0] = null;
                case 0:
                    break;
                default:
                    elements[0] = null;
                    do {
                        System.arraycopy(elements, 0, elements, index, index);
                    } while ((index <<= 1) < size);
                    System.arraycopy(elements, 0, elements, index >>>= 1, size - index);
            }
        } finally {
            unlock();
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return _add(-1, c.toArray());
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(bounds(index, _size.get()));
        }
        return _add(index, c.toArray());
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = false;
        for (Object o : c) {
            result |= remove(o);
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        acquireLock();

        try {
            boolean result = false;

            for (int i = _size.get(); i > 0; ) {
                if (!c.contains(_elements[--i])) {
                    System.arraycopy(_elements, i + 1, _elements, i, _size.getAndDecrement() - i);
                    result = true;
                }
            }

            return result;
        } finally {
            unlock();
        }
    }

    private boolean _add(int index, Object[] array) {
        if (array.length == 0) {
            return false;
        }
        acquireLock();
        try {
            int size = _size.get(), length = array.length, newSize;
            if (index == -1) {
                index = size;
            } else if (index > size) {
                throw new IndexOutOfBoundsException(bounds(index, size));
            }
            Object[] elements = _elements;
            if ((newSize = size + array.length) > elements.length) {
                System.arraycopy(elements, 0, elements = new Object[Math.max(newSize, elements.length << 1)], 0, index);
                System.arraycopy(_elements, index, _elements = elements, index + length, size - index);
            } else {
                System.arraycopy(elements, index, elements, index + length, size - index);
            }
            System.arraycopy(array, 0, elements, index, length);
            return true;
        } finally {
            unlock();
        }
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        }
        if (toIndex > _size.get()) {
            throw new IllegalArgumentException("toIndex = " + toIndex);
        }
        if (toIndex < fromIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
        return new SubList(fromIndex, toIndex);
    }

    @Override
    public ListIterator<E> listIterator() {
        return new Itr<>(this, 0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        int size;
        if (index > (size = _size.get()) || index < 0) {
            throw new IndexOutOfBoundsException(bounds(index, size));
        }
        return new Itr<>(this, index);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (E e : this) {
            hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof List)) {
            return false;
        }

        ListIterator a = listIterator(),
                     b = ((List) obj).listIterator();
        while (a.hasNext() && b.hasNext()) {
            Object ele = a.next();
            if (!(ele == null ? b.next() == null : ele.equals(b.next()))) {
                return false;
            }
        }
        return !(a.hasNext() || b.hasNext());
    }

    private static class Itr<E> implements ListIterator<E>, Iterator<E> {

        final List<E> list;

        int cursor = -1;
        E next, prev;

        boolean hasNext, hasPrev;

        Itr(List<E> list, int index) {
            this.list = list;
            cursor = index - 1;
        }

        @Override
        public boolean hasNext() {
            if (hasNext) {
                return true;
            }
            try {
                next = list.get(cursor + 1);
                return hasNext = true;
            } catch (IndexOutOfBoundsException ex) {
                return hasNext = false;
            }
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            E next = this.next;
            this.next = prev = null;
            hasNext = hasPrev = false;
            ++cursor;
            return next;
        }

        @Override
        public boolean hasPrevious() {
            if (hasPrev) {
                return true;
            }
            try {
                prev = list.get(cursor);
                return hasPrev = true;
            } catch (IndexOutOfBoundsException ex) {
                return hasPrev = false;
            }
        }

        @Override
        public E previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            E prev = this.prev;
            next = this.prev = null;
            hasNext = hasPrev = false;
            --cursor;
            return prev;
        }

        @Override
        public int nextIndex() {
            return cursor + 1;
        }

        @Override
        public int previousIndex() {
            return cursor;
        }

        @Override
        public void remove() {
            if (hasNext || hasPrev) {
                list.remove(cursor);
                next = prev = null;
                hasNext = hasPrev = false;
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public void set(E e) {
            if (next != null) {
                list.set(cursor, e);
                next = null;
            } else if (prev != null) {
                list.set(cursor - 1, e);
                prev = null;
            }
        }

        @Override
        public void add(E e) {
            list.add(cursor, e);
            cursor++;
        }
    }

    private class SubList implements List<E> {

        private final int from;
        private final AtomicInteger _count;

        private SubList(int from, int to) {
            this.from = from;
            _count = new AtomicInteger(to - from);
        }

        @Override
        public int size() {
            int i = _size.get() - from;
            return i < 0 ? 0 : Math.min(i, _count.get());
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public boolean contains(Object o) {
            return indexOf(o) >= 0;
        }

        @Override
        public Iterator<E> iterator() {
            return new Itr<>(this, 0);
        }

        @Override
        public Object[] toArray() {
            acquireLock();
            try {
                int size = size();
                Object[] result = new Object[size];
                if (size > 0) {
                    System.arraycopy(_elements, from, result, 0, size);
                }
                return result;
            } finally {
                unlock();
            }
        }

        @Override
        public <T> T[] toArray(T[] a) {
            acquireLock();

            try {
                return _toArray(a, from, size());
            } finally {
                unlock();
            }
        }

        @Override
        public boolean add(E e) {
            try {
                ConcurrentArrayList.this.add(from + _count.get(), e);
            } catch (IndexOutOfBoundsException ex) {
                ConcurrentArrayList.this.add(e);
            }
            return true;
        }

        @Override
        public boolean remove(Object o) {
            int end = from + _count.get();
            return _remove(o, from, end >= _size.get() ? -1 : end);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            int index, size = size();
            if (size == 0) {
                return c.isEmpty();
            }
            for (Object o : c) {
                if ((index = ConcurrentArrayList.this.indexOf(o)) < from || index >= size) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            Object[] array = c.toArray();
            try {
                return _add(from + _count.getAndAdd(array.length), array);
            } catch (IndexOutOfBoundsException ex) {
                return _add(-1, array);
            }
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            int size = size();
            if (index < 0 || index > size) {
                throw new IndexOutOfBoundsException(bounds(index, size));
            }
            return _add(index, c.toArray());
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            boolean result = false;
            for (Object o : c) {
                result |= remove(o);
            }
            return result;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return removeIf(e -> !c.contains(e));
        }

        @Override
        public void clear() {
            acquireLock();

            try {
                int size = _count.getAndSet(0);
                if (size > 0) {
                    Object[] elements = _elements;
                    int src = Math.min(from + size, size = _size.get());
                    System.arraycopy(elements, src, elements, from, elements.length - src);
                    for (int i = src; i < size; ) {
                        elements[i++] = null;
                    }
                }
            } finally {
                unlock();
            }
        }

        @Override
        public E get(int index) {
            int size = size();
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException(bounds(index, size));
            }
            return ConcurrentArrayList.this.get(from + index);
        }

        @Override
        public E set(int index, E element) {
            int size = size();
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException(bounds(index, size));
            }
            return ConcurrentArrayList.this.set(from + index, element);
        }

        @Override
        public void add(int index, E element) {
            int size = size();
            if (index < 0 || index > size) {
                throw new IndexOutOfBoundsException(bounds(index, size));
            }
            ConcurrentArrayList.this.add(from + index, element);
        }

        @Override
        public E remove(int index) {
            int size = size();
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException(bounds(index, size));
            }
            return ConcurrentArrayList.this.remove(from + index);
        }

        @Override
        public int indexOf(Object o) {
            int index = ConcurrentArrayList.this.indexOf(o) - from;
            return index >= 0 && index < size() ? index : -1;
        }

        @Override
        public int lastIndexOf(Object o) {
            int index = ConcurrentArrayList.this.lastIndexOf(o) - from;
            return index >= 0 && index < size() ? index : -1;
        }

        @Override
        public ListIterator<E> listIterator() {
            return new Itr<>(this, 0);
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            int size;
            if (index > (size = size()) || index < 0) {
                throw new IndexOutOfBoundsException(bounds(index, size));
            }
            return new Itr<>(this, index);
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            if (fromIndex < 0) {
                throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
            }
            if (toIndex > size()) {
                throw new IllegalArgumentException("toIndex = " + toIndex);
            }
            if (toIndex < fromIndex) {
                throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
            }
            return new SubList(fromIndex, toIndex);
        }
    }
}
