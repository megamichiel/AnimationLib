package me.megamichiel.animationlib.animation;

import me.megamichiel.animationlib.Nagger;

public class AnimatedNumber extends Animatable<Integer> {
	
	private static final long serialVersionUID = 9008416363369565560L;
	
	@Override
	protected Integer convert(Nagger nagger, String str) {
		try
		{
			return Integer.valueOf(str);
		}
		catch (NumberFormatException ex)
		{
			nagger.nag("Failed to parse number '" + str + "'!");
			nagger.nag(ex);
		}
		return defaultValue();
	}
	
	@Override
	protected Integer defaultValue() {
		return 0;
	}
}
