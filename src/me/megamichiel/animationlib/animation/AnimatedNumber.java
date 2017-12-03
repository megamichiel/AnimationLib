package me.megamichiel.animationlib.animation;

import me.megamichiel.animationlib.Nagger;

import java.util.function.Function;

/**
 * @deprecated use {@link AbsAnimatable#ofNumber(Function)}
 */
@Deprecated
public class AnimatedNumber extends Animatable<Integer> {
    
    @Override
    protected Integer convert(Nagger nagger, Object o) {
        try {
            return Integer.valueOf(o.toString());
        } catch (NumberFormatException ex) {
            nagger.nag("Failed to parse number '" + o + "'!");
            nagger.nag(ex);
        }
        return null;
    }
}
