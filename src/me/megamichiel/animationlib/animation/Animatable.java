package me.megamichiel.animationlib.animation;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.config.AbstractConfig;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class that moves through multiple values using 'frames'
 *
 * @param <E> the type of this animatable
 * @deprecated use {@link AbsAnimatable}
 */
@Deprecated
public abstract class Animatable<E> extends ArrayList<E> implements IAnimatable<E> {

    public static <E> Animatable<E> of(E def, BiFunction<Nagger, String, E> func) {
        return new Animatable<E>() {
            @Override
            protected E convert(Nagger nagger, Object o) {
                return func.apply(nagger, o.toString());
            }

            @Override
            protected E defaultValue() {
                return def;
            }
        };
    }

    public static List<Animatable<?>> filterAnimated(Animatable<?>... animatables) {
        return Stream.of(animatables).filter(Animatable::isAnimated).collect(Collectors.toList());
    }

    private int frame = 0, interval, tick;
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
     * @return whether {@link #size()} is > 1
     */
    @Override
    public boolean isAnimated() {
        return size() > 1;
    }

    /**
     * Returns the value at the current frame
     *
     * @return the value at the current frame, or {@link #defaultValue()} if this Animatable is empty
     */
    @Override
    public E get() {
        return isEmpty() ? defaultValue() : get(frame);
    }

    /**
     * Returns the value retrieved by {@link #get()}, and moves to the next frame
     *
     * @deprecated {@link #tick()} should be used
     */
    @Deprecated
    public E next() {
        E value = get();
        tick();
        return value;
    }

    @Override
    public boolean tick() {
        if (--tick < 0) {
            tick = interval;
            int size = size();
            switch (size) {
                case 2:
                    frame = 1 - frame; // Ezpz 2 frames
                case 0: case 1:
                    break;
                default:
                    if (isRandom) {
                        // No frames twice in a row ;3
                        int prev = frame;
                        do {
                            frame = ThreadLocalRandom.current().nextInt(size);
                        } while (frame == prev);
                    } else if (++frame >= size) {
                        frame = 0;
                    }
            }
            return true;
        }
        return false;
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

    public void setDelay(int delay) {
        interval = tick = Math.max(delay, 0);
    }

    public boolean load(Nagger nagger, String path, AbstractConfig section) {
        Map<Integer, Object> values = new HashMap<>();
        int highest = 1;
        List<String> errors = new ArrayList<>();
        for (String id : section.keys()) {
            switch (id) {
                case "random":
                    isRandom = section.getBoolean(id);
                case "delay":
                    break;
                default:
                    Object value = getValue(nagger, section, id);
                    if (value == null) {
                        continue;
                    }
                    for (String item : id.split(",")) {
                        item = item.trim();
                        try {
                            int num = Integer.parseInt(item);
                            if (num > 0) {
                                if (num > highest) {
                                    highest = num;
                                }
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
                                    if (max > highest) {
                                        highest = max;
                                    }
                                    for (int i = min; i <= max; i++) {
                                        values.put(i, value);
                                    }
                                } catch (NumberFormatException ex2) {
                                    errors.add("Invalid number: " + item + '!');
                                }
                            }
                        }
                    }
            }
        }
        errors.forEach(nagger::nag);
        Object last = null;
        for (int i = 1; i <= highest; i++) {
            Object o = values.get(i);
            if (o != null) {
                last = o;
            } else if (last == null) {
                nagger.nag("No frame specified at frame " + i + " in " + path + "!");
            }
            E e = convert(nagger, last);
            if (e != null) {
                add(e);
            }
        }
        return true;
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
            return load(nagger, key, section.getSection(key));
        }
        Object value = getValue(nagger, section, key);
        if (value != null) {
            E converted = convert(nagger, value);
            if (converted != null) {
                add(converted);
                return true;
            }
        }
        return false;
    }

    protected Object getValue(Nagger nagger, AbstractConfig section, String key) {
        return section.getString(key);
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
