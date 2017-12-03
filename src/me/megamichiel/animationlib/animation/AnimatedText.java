package me.megamichiel.animationlib.animation;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.StringBundle;

/**
 * An {@link Animatable} made for {@link StringBundle StringBundles}
 * @deprecated use {@link AbsAnimatable#ofText(boolean)}
 */
@Deprecated
public class AnimatedText extends Animatable<StringBundle> {

    public AnimatedText() { }

    @Override
    protected StringBundle convert(Nagger nagger, Object object) {
        return StringBundle.parse(nagger, object.toString()).colorAmpersands();
    }
}
