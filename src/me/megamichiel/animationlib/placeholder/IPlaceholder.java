package me.megamichiel.animationlib.placeholder;

import me.megamichiel.animationlib.Nagger;

/**
 * An interface which can be used to retrieve values from using a player
 *
 * @param <T> the type of the placeholder
 */
public interface IPlaceholder<T> {

    static <T> IPlaceholder<T> constant(T value) {
        return (nagger, who) -> value;
    }

    /**
     * Returns a value using a player
     *
     * @param nagger the nagger to report errors to
     * @param who the player to do stuff with
     * @return a value
     */
    T invoke(Nagger nagger, Object who);

    default T invoke(Nagger nagger, Object who, PlaceholderContext ctx) {
        return invoke(nagger, who);
    }
}
