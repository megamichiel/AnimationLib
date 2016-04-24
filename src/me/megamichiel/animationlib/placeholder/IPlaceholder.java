package me.megamichiel.animationlib.placeholder;

import me.megamichiel.animationlib.Nagger;
import org.bukkit.entity.Player;

/**
 * An interface which can be used to retrieve values from using a {@link Player}
 *
 * @param <T> the type of the placeholder
 */
public interface IPlaceholder<T> {
    
    T invoke(Nagger nagger, Player who);

    /**
     * A convenience Placeholder class which holds a single value that it always retrieves
     *
     * @param <T>
     */
    final class ConstantPlaceholder<T> implements IPlaceholder<T> {

        /**
         * Creates a new ConstantPlaceholder, with T as value
         *
         * @param value the value to retrieve in {@link IPlaceholder#invoke(Nagger, Player)}
         * @param <T> the type of the value
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
        public T invoke(Nagger nagger, Player who) {
            return value;
        }
    }
}
