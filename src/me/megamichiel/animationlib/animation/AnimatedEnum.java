package me.megamichiel.animationlib.animation;

import com.google.common.base.Joiner;
import me.megamichiel.animationlib.Nagger;

import java.util.Locale;

/**
 * An Animatable to be used with Enum values
 *
 * @param <E> the Enum type
 */
public class AnimatedEnum<E extends Enum<E>> extends Animatable<E> {

    /**
     * Creates a new AnimatedEnum from <i>clazz</i>
     *
     * @param clazz the type to create an AnimatedEnum from
     * @param <E> the type of <i>clazz</i>
     * @return a newly creates AnimatedEnum
     */
    public static <E extends Enum<E>> AnimatedEnum<E> of(Class<E> clazz) {
        return new AnimatedEnum<>(clazz);
    }
    
    private static final long serialVersionUID = 4105836255454150188L;
    private final Class<E> clazz;

    /**
     * @see #of(Class)
     */
    public AnimatedEnum(Class<E> clazz) {
        this.clazz = clazz;
    }

    @Override
    public AnimatedEnum<E> clone() {
        return (AnimatedEnum<E>) super.clone();
    }

    @Override
    protected E convert(Nagger nagger, Object str) {
        try {
            return Enum.valueOf(clazz, str.toString().toUpperCase(Locale.US).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            nagger.nag("Couldn't find " + clazz.getSimpleName() + " by id " + str + '!');
            nagger.nag("Possible values: " + Joiner.on(", ").join(clazz.getEnumConstants()).toLowerCase().replace('_', '-'));
            return defaultValue();
        }
    }
}
