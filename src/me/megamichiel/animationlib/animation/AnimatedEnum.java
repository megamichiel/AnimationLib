package me.megamichiel.animationlib.animation;

import com.google.common.base.Joiner;
import me.megamichiel.animationlib.Nagger;

import java.util.Locale;

/**
 * An Animatable to be used with Enum values
 *
 * @param <E> the Enum type
 * @deprecated use {@link AbsAnimatable#ofEnum(Class)}
 */
@Deprecated
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

    private final Class<E> clazz;

    /**
     * @see #of(Class)
     */
    public AnimatedEnum(Class<E> clazz) {
        this.clazz = clazz;
    }

    @Override
    protected E convert(Nagger nagger, Object o) {
        try {
            return Enum.valueOf(clazz, o.toString().toUpperCase(Locale.US).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            nagger.nag("Couldn't find " + clazz.getSimpleName() + " by id " + o + '!');
            nagger.nag("Possible values: " + Joiner.on(", ").join(clazz.getEnumConstants()).toLowerCase().replace('_', '-'));
            return null;
        }
    }
}
