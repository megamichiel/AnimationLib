package me.megamichiel.animationlib.animation;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.config.AbstractConfig;

import java.util.*;

/**
 * A class that moves through multiple values using 'frames'
 *
 * @param <E> the type of this animatable
 */
public abstract class Animatable<E> extends ArrayList<E> {
    
    private static final long serialVersionUID = -7324365301382371283L;
    private static final Random random = new Random();

    private int frame = 0;
    protected E defaultValue;
    protected boolean isRandom;

    public Animatable() {}
    
    public Animatable(Collection<? extends E> c)
    {
        super(c);
    }
    
    public Animatable(E[] elements)
    {
        super(Arrays.asList(elements));
    }

    /**
     * Returns the value at the current frame
     *
     * @return the value at the current frame, or {@link #defaultValue()} if this Animatable is empty
     */
    public E get() {
        return isEmpty() ? defaultValue() : get(frame);
    }

    /**
     * @return whether {@link #size()} is > 1
     */
    public boolean isAnimated() {
        return size() > 1;
    }

    /**
     * Returns the value retrieved by {@link #get()}, and moves to the next frame
     */
    public E next() {
        E current = get();
        switch (size()) {
            case 0: case 1: return current;
            case 2:
                frame = 1 - frame; // Ezpz 2 frames
                return current;
            default:
                if (isRandom) {
                    // No frames twice in a row ;3
                    int size = size(), prev = frame;
                    do {
                        frame = random.nextInt(size);
                    } while (frame == prev);
                } else if (++frame == size()) frame = 0;
                return current;
        }
    }

    /**
     * Converts an Object to <i>E</i>
     *
     * @param nagger the {@link Nagger} to report warnings to
     * @param o the Object to convert to <i>E</i>
     * @return the value created
     */
    protected E convert(Nagger nagger, Object o) {
        return null;
    }

    /**
     * Returns the default value, which is retrieved by {@link #get(int)} if this Animatable is empty
     */
    protected E defaultValue() {
        return defaultValue;
    }

    /**
     * Loads this Animatable from <i>section</i> with key <i>key</i>
     *
     * @param nagger the {@link Nagger} to report warnings to
     * @param section the configuration section to get the value from
     * @param key the key to get the value from
     * @return true if this Animatable has loaded at least 1 value
     */
    public boolean load(Nagger nagger, AbstractConfig section, String key) {
        return load(nagger, section, key, null);
    }

    /**
     * Returns whether this Animatable iterates over the values randomly
     */
    public boolean isRandom() {
        return isRandom;
    }

    /**
     * Sets whether this Animatable should iterate over the values randomly
     */
    public void setRandom(boolean random) {
        isRandom = random;
    }

    /**
     * Returns whether this Animatable's value is a section (can account for it)
     */
    protected boolean isSection() {
        return false;
    }

    /**
     * Loads this Animatable from <i>section</i> with key <i>key</i>
     *
     * @param nagger the {@link Nagger} to report warnings to
     * @param section the configuration section to get the value from
     * @param key the key to get the value from
     * @param defaultValue the value to retrieve if this Animatable is empty. Retrieved by the default {@link #defaultValue()}
     *
     * @return true if this Animatable has loaded at least 1 value
     */
    public boolean load(Nagger nagger, AbstractConfig section, String key, E defaultValue) {
        this.defaultValue = defaultValue;
        if (section.isSection(key) && (!isSection() || section.getBoolean("animate-" + key))) {
            AbstractConfig sec = section.getSection(key);
            Map<Integer, Object> values = new HashMap<>();
            int highest = 1;
            List<String> errors = new ArrayList<>();
            for (String id : sec.keys()) {
                if ("random".equals(id)) {
                    isRandom = sec.getBoolean(id);
                    continue;
                }
                Object value = getValue(nagger, sec, id);
                if (value == null) continue;
                for (String item : id.split(",")) {
                    item = item.trim();
                    try {
                        int num = Integer.parseInt(item);
                        if (num > 0) {
                            if (num > highest) highest = num;
                            values.put(num, value);
                        }
                    } catch (NumberFormatException ex) {
                        int index = item.indexOf('-');
                        if (index > 0 && index < item.length() - 1) {
                            try {
                                int min = Integer.parseInt(item.substring(0, index)),
                                        max = Integer.parseInt(item.substring(index + 1));
                                if (max < min) {
                                    errors.add("Max < Min at " + item + "!");
                                    continue;
                                }
                                if (max > highest) highest = max;
                                for (int i = min; i <= max; i++) values.put(i, value);
                            } catch (NumberFormatException ex2) {
                                errors.add("Invalid number: " + item + '!');
                            }
                        }
                    }
                }
            }
            errors.forEach(nagger::nag);
            Object last = null;
            for (int i = 1; i <= highest; i++) {
                Object o = values.get(i);
                if (o != null) last = o;
                else if (last == null)
                    nagger.nag("No frame specified at " + i + " in " + key + "!");
                E e = convert(nagger, last);
                if (e != null) add(e);
            }
            return true;
        }
        Object value = getValue(nagger, section, key);
        if (value != null) {
            add(convert(nagger, value));
            return true;
        }
        return false;
    }

    protected Object getValue(Nagger nagger, AbstractConfig section, String key) {
        return section.getString(key);
    }

    @Override
    public Animatable<E> clone() {
        return (Animatable<E>) super.clone();
    }

    public void copyTo(Animatable<E> other) {
        other.addAll(this);
        other.defaultValue = defaultValue;
        other.isRandom = isRandom;
    }

    public static <E, A extends Animatable<E>> A load(A animatable, Nagger nagger,
                                                      AbstractConfig section, String key, E def) {
        animatable.load(nagger, section, key, def);
        return animatable;
    }
}
