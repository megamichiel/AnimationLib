package me.megamichiel.animationlib.placeholder;

import me.megamichiel.animationlib.Nagger;
import org.bukkit.entity.Player;

/**
 * A convenience {@link IPlaceholder}, which retrieves an Integer
 *
 */
public class NumberPlaceholder implements IPlaceholder<Integer> {
    
    public static final NumberPlaceholder ZERO = of(0);

    public static NumberPlaceholder of(int val)
    {
        return new NumberPlaceholder(new ConstantPlaceholder<>(val));
    }
    
    private final IPlaceholder<Integer> handle;

    /**
     * Creates a new NumberPlaceholder, which uses <i>handle</i> to obtain values
     *
     * @param handle the {@link IPlaceholder} to obtain values from
     */
    public NumberPlaceholder(IPlaceholder<Integer> handle) {
        this.handle = handle;
    }

    /**
     * Creates a new NumberPlaceholder, which either parses a constant number or a placeholder
     *
     * @param string the value to parse
     * @throws IllegalArgumentException if the value is neither an integer nor a placeholder
     */
    public NumberPlaceholder(String string) throws IllegalArgumentException {
        IPlaceholder<Integer> placeholder = null;
        try {
            final int val = Integer.parseInt(string);
            placeholder = ConstantPlaceholder.of(val);
        } catch (NumberFormatException ex) {
            if (string.startsWith("%") && string.endsWith("%")) {
                final Placeholder ph = new Placeholder(string.substring(1, string.length() - 1));
                placeholder = new IPlaceholder<Integer>() {
                    @Override
                    public Integer invoke(Nagger nagger, Player who) {
                        String result = ph.invoke(nagger, who);
                        try {
                            return Integer.parseInt(result);
                        } catch (NumberFormatException ex) {
                            nagger.nag("Placeholder " + ph.toString() + " didn't return a number! Got '" + result + "' instead");
                            return 0;
                        }
                    }
                };
            }
            if (placeholder == null) {
                throw new IllegalArgumentException(string);
            }
        }
        handle = placeholder;
    }
    
    @Override
    public Integer invoke(Nagger nagger, Player who) {
        return handle.invoke(nagger, who);
    }
}
