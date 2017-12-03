package me.megamichiel.animationlib.animation;

public interface IAnimatable<E> {

    static <E> IAnimatable<E> single(E value) {
        return () -> value;
    }

    static IAnimatable<Integer> timer(int delay) {
        return delay < 0 ? single(0) : new IAnimatable<Integer>() {
            int tick = delay;

            @Override
            public Integer get() {
                return tick;
            }

            @Override
            public boolean isAnimated() {
                return true;
            }

            @Override
            public boolean tick() {
                if (--tick < 0) {
                    tick = delay;
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * Returns the value at the current frame
     */
    E get();

    /**
     * Returns whether this IAnimatable actually animates.
     * By default, this method returns false. Returning true while it isn't actually animated is fine, but may be slightly less efficient.
     * If the method returns false, but the instance ís animatable, {@link #tick()} won't be called and this will never update, if the system is properly optimized.
     */
    default boolean isAnimated() {
        return false;
    }

    /**
     * Called every tick on a registered IAnimatable (may very by context). The default implementation returns false.
     * If overridden, this method should return true if this IAnimatable instance has changed value as a result of the call.
     */
    default boolean tick() {
        return false;
    }
}
