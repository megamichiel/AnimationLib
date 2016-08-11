package me.megamichiel.animationlib.animation;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.StringBundle;

import java.util.Collection;

/**
 * An {@link Animatable} made for {@link StringBundle StringBundles}
 *
 */
public class AnimatedText extends Animatable<StringBundle> {
    
    private static final long serialVersionUID = 5235518796395129933L;

    public AnimatedText() {}
    
    public AnimatedText(Collection<? extends StringBundle> c) {
        super(c);
    }
    
    public AnimatedText(StringBundle... elements) {
        super(elements);
    }

    @Override
    public AnimatedText clone() {
        AnimatedText txt = (AnimatedText) super.clone();
        for (int i = txt.size(); i-- != 0;)
            txt.set(i, txt.get(i).clone());
        txt.defaultValue = defaultValue();
        txt.isRandom = isRandom;
        return txt;
    }

    @Override
    protected StringBundle convert(Nagger nagger, Object str) {
        return StringBundle.parse(nagger, str.toString()).colorAmpersands();
    }
}
