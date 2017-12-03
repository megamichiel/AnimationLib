package me.megamichiel.animationlib.placeholder;

import me.megamichiel.animationlib.Nagger;

/**
 * A convenience {@link IPlaceholder}, which retrieves an Integer
 */
public class NumberPlaceholder implements IPlaceholder<Integer> {
    
    public static final NumberPlaceholder ZERO = of(0);

    public static NumberPlaceholder of(int val) {
        return new NumberPlaceholder(CtxPlaceholder.constant(val));
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
        IPlaceholder<Integer> placeholder;
        try {
            int val = Integer.parseInt(string);
            placeholder = CtxPlaceholder.constant(val);
        } catch (NumberFormatException ex) {
            StringBundle sb = StringBundle.parse(Nagger.ILLEGAL_ARGUMENT, string);
            if (!sb.containsPlaceholders()) {
                throw new IllegalArgumentException(string + " is not a number!");
            }
            placeholder = (CtxPlaceholder<Integer>) (nagger, who, ctx) -> {
                String result = sb.invoke(nagger, who, ctx);
                try {
                    return Integer.valueOf(result);
                } catch (NumberFormatException e) {
                    nagger.nag("Expected number, got " + result);
                    return 0;
                }
            };
        }
        handle = placeholder;
    }

    public IPlaceholder<Integer> getHandle() {
        return handle;
    }

    @Override
    public Integer invoke(Nagger nagger, Object who) {
        return handle.invoke(nagger, who);
    }

    @Override
    public Integer invoke(Nagger nagger, Object who, PlaceholderContext ctx) {
        return handle.invoke(nagger, who, ctx);
    }
}
