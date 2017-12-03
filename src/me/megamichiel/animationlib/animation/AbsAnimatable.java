package me.megamichiel.animationlib.animation;

import com.google.common.base.Joiner;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.config.ConfigSection;
import me.megamichiel.animationlib.placeholder.StringBundle;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AbsAnimatable<E> extends ArrayList<E> implements IAnimatable<E> {

    public static <E> AbsAnimatable<E> of(BiFunction<Nagger, Object, E> func) {
        return new AbsAnimatable<E>() {
            @Override
            protected E get(Nagger nagger, Object o) {
                return func.apply(nagger, o);
            }
        };
    }

    public static <E extends Enum<E>> AbsAnimatable<E> ofEnum(Class<E> type) {
        return new AbsAnimatable<E>() {
            @Override
            protected E get(Nagger nagger, Object o) {
                try {
                    return Enum.valueOf(type, o.toString().toUpperCase(Locale.US).replace('-', '_'));
                } catch (IllegalArgumentException ex) {
                    nagger.nag("Couldn't find " + type.getSimpleName() + " by id " + o + '!');
                    nagger.nag("Possible values: " + Joiner.on(", ").join(type.getEnumConstants()).toLowerCase().replace('_', '-'));
                    return null;
                }
            }
        };
    }

    public static <N extends Number> AbsAnimatable<N> ofNumber(Function<String, N> parser) {
        return new AbsAnimatable<N>() {
            @Override
            protected N get(Nagger nagger, Object o) {
                try {
                    return parser.apply(o.toString());
                } catch (NumberFormatException ex) {
                    nagger.nag("Failed to parse number '" + o + "'!");
                    nagger.nag(ex);
                }
                return null;
            }
        };
    }

    public static AbsAnimatable<StringBundle> ofText(boolean tickText) {
        return new AbsAnimatable<StringBundle>() {
            @Override
            public boolean isAnimated() {
                if (super.isAnimated()) {
                    return true;
                }
                if (tickText) {
                    for (StringBundle bundle : this) {
                        if (bundle.isAnimated()) {
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public boolean tick() {
                boolean result = super.tick();
                if (tickText) {
                    for (StringBundle bundle : this) {
                        result |= bundle.tick();
                    }
                }
                return result;
            }

            @Override
            protected StringBundle get(Nagger nagger, Object o) {
                return StringBundle.parse(nagger, o.toString()).colorAmpersands();
            }
        };
    }

    public static void loadAll(Nagger nagger, ConfigSection section, int delay, Object[] values) {
        for (int i = 0; i < values.length; ) {
            String key = (String) values[i++];
            AbsAnimatable value = (AbsAnimatable) values[i++];
            if (value.load(nagger, section, key).size() > 1 && delay >= 0) {
                value.setDelay(section.getSection(key).getInt("delay", delay));
            }
            value.addDefault(values[i++]);
        }
    }

    private final boolean section;

    private int frame, delay, tick;

    private boolean random;

    public AbsAnimatable() {
        this(false);
    }

    public AbsAnimatable(boolean section) {
        this.section = section;
    }

    @Override
    public boolean isAnimated() {
        return size() > 1;
    }

    @Override
    public E get() {
        return isEmpty() ? null : get(frame);
    }

    @Override
    public boolean tick() {
        if (--tick < 0) {
            tick = delay;
            int size = size();
            switch (size) {
                case 2:
                    frame = 1 - frame; // Ezpz 2 frames
                case 0: case 1:
                    break;
                default:
                    if (random) {
                        // No frames twice in a row ;3
                        for (int prev = frame; ; ) {
                            if (prev != (prev = ThreadLocalRandom.current().nextInt(size))) {
                                frame = prev;
                                break;
                            }
                        }
                    } else if (++frame >= size) {
                        frame = 0;
                    }
            }
            return true;
        }
        return false;
    }

    public void setDelay(int delay) {
        this.delay = this.tick = Math.max(delay, 0);
    }

    public void setRandom(boolean random) {
        this.random = random;
    }

    protected abstract E get(Nagger nagger, Object object);

    public AbsAnimatable<E> load(Nagger nagger, String path, ConfigSection section) {
        int highest = 1;

        Map<Integer, E> values = new HashMap<>();
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, Object> entry : section.values().entrySet()) {
            String id = entry.getKey();
            Object value = entry.getValue();
            switch (id) {
                case "random":
                    random = (value instanceof Boolean && (Boolean) value) || "true".equalsIgnoreCase(value.toString());
                case "delay":
                    break;
                default:
                    E e = get(nagger, value);
                    if (e == null) {
                        continue;
                    }
                    for (String item : id.split(",")) {
                        int index = (item = item.trim()).indexOf('-');
                        try {
                            int num = Integer.parseInt(item.substring(index + 1));
                            if (index >= 0) {
                                int min = Integer.parseInt(item.substring(0, index));
                                if (num < min) {
                                    errors.add("Max < Min at " + item + "!");
                                    continue;
                                }
                                if (num > highest) {
                                    highest = num;
                                }
                                for (int i = min; i <= num; ) {
                                    values.put(i++, e);
                                }
                            } else {
                                if (num > 0) {
                                    if (num > highest) {
                                        highest = num;
                                    }
                                    values.put(num, e);
                                }
                            }
                        } catch (NumberFormatException ex) {
                            errors.add("Invalid frame index: " + item + '!');
                        }
                    }
            }
        }
        errors.forEach(nagger::nag);
        E last = null;
        for (int i = 1; i <= highest; i++) {
            E e = values.get(i);
            if (e != null) {
                last = e;
            } else if (last == null) {
                nagger.nag("No frame specified at frame " + i + " in " + path + "!");
                continue;
            }
            add(last);
        }
        return this;
    }

    public AbsAnimatable<E> load(Nagger nagger, ConfigSection section, String key) {
        Object value = section.get(key); E e;
        if (value instanceof ConfigSection && (!this.section || section.getBoolean("animate-" + key))) {
            return load(nagger, key, section.getSection(key));
        }
        if (value != null && (e = get(nagger, value)) != null) {
            add(e);
        }
        return this;
    }

    public boolean addDefault(E defaultValue) {
        if (isEmpty()) {
            if (defaultValue != null) {
                add(defaultValue);
            }
            return true;
        }
        return false;
    }
}
