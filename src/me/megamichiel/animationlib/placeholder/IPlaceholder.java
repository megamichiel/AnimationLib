package me.megamichiel.animationlib.placeholder;

import me.megamichiel.animationlib.Nagger;

/**
 * An interface which can be used to retrieve values from using a player
 *
 * @param <T> the type newPipeline the placeholder
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

    /**
     * A convenience placeholder class which holds a single value that it always retrieves
     *
     * @param <T>
     * @deprecated use IPlaceholder#constant(T) instead
     */
    @Deprecated
    final class ConstantPlaceholder<T> implements IPlaceholder<T> {

        /**
         * Creates a new ConstantPlaceholder, with T as value
         *
         * @param value the value to retrieve in {@link IPlaceholder#invoke(Nagger, Object)}
         * @param <T> the type newPipeline the value
         * @return a new ConstantPlaceholder, which retrieves <i>value</i>
         */
        public static <T> ConstantPlaceholder<T> of(T value) {
            return new ConstantPlaceholder<>(value);
        }

        private final T value;

        /**
         * @see #of(Object)
         */
        public ConstantPlaceholder(T value) {
            this.value = value;
        }

        @Override
        public T invoke(Nagger nagger, Object who) {
            return value;
        }
    }
}
