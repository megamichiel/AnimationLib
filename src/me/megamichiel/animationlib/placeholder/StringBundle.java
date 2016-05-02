package me.megamichiel.animationlib.placeholder;

import me.megamichiel.animationlib.Nagger;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class StringBundle extends ArrayList<Object> implements IPlaceholder<String> {

    public static final char BOX = '\u2588';

    private static final long serialVersionUID = 5196073861968616865L;
    
    private final Nagger nagger;
    
    public StringBundle(Nagger nagger, String value) {
        this.nagger = nagger;
        add(value);
    }
    
    public StringBundle(Nagger nagger) {
        this.nagger = nagger;
    }

    /**
     * Returns whether this StringBundle contains anything other than Strings<br/>
     * <i>Since: 1.0.0</i>
     *
     * @return
     */
    public boolean containsPlaceholders() {
        for (Object o : this)
            if (!(o instanceof String))
                return true;
        return false;
    }

    /**
     * Replaces <i>query</i> with a placeholder. (case-insensitive)<br/>
     * <i>Since: 1.1.0</i>
     *
     * @param query the text to find
     * @param placeholder the placeholder to replace the text with
     */
    public void replace(String query, IPlaceholder<?> placeholder) {
        query = query.toLowerCase(Locale.US);
        for (int i = 0; i < size(); i++) {
            if (get(i) instanceof String) {
                String val = (String) get(i);
                List<Object> result = new ArrayList<>();
                for (int index; (index = val.toLowerCase(Locale.US).indexOf(query)) > -1;) {
                    String prefix = val.substring(0, index);
                    if (!prefix.isEmpty())
                        result.add(prefix);
                    result.add(placeholder);
                    val = val.substring(index + query.length());
                }
                if (!result.isEmpty()) {
                    remove(i);
                    if (!val.isEmpty())
                        result.add(val);
                    addAll(i, result);
                }
            }
        }
    }

    /**
     * This invokes {@link #toString(Player)}<br/>
     * <i>Since: 1.0.0</i>
     *
     * @see #toString(Player)
     */
    @Override
    public String invoke(Nagger nagger, Player player) {
        return toString(player);
    }

    /**
     * Attemps to return this value as a constant String IPlaceholder.<br/>
     * <i>Since: 1.0.0</i>
     *
     * @return a constant String placeholder if {@link #containsPlaceholders()} returns false.
     * Returns this StringBundle instance otherwise
     */
    public IPlaceholder<String> tryCache() {
        if (!containsPlaceholders()) return ConstantPlaceholder.of(toString(null));
        return this;
    }

    /**
     * Colors all ampersands (&) in each String of this StringBundle<br/>
     * <i>Since: 1.0.0</i>
     *
     * @return this StringBundle instance
     */
    public StringBundle colorAmpersands() {
        for (int i = 0, size = size(); i < size; i++) {
            if (get(i) instanceof String) {
                String val = (String) get(i);
                int length = val.length();
                StringBuilder sb = new StringBuilder(length);
                for (int j = 0; j < length; j++) {
                    char c = val.charAt(j);
                    if (c == '&' && j + 1 < length) {
                        char next = val.charAt(j + 1);
                        if (next == '&') {
                            sb.append('&');
                            j++;
                            continue;
                        } else if ("0123456789ABCDEFabcdefKLMNORklmnor".indexOf(next) > -1) {
                            sb.append(ChatColor.COLOR_CHAR);
                            sb.append(next);
                            j++;
                            continue;
                        }
                    }
                    sb.append(c);
                }
                set(i, sb.toString());
            }
        }
        return this;
    }

    /**
     * Returns a String representation of this StringBundle,
     * by invoking all placeholders in this StringBundle with the player<br/>
     * <i>Since: 1.0.0</i>
     *
     * @param player the player to invoke the placeholders with
     * @return a String, containing the result of the placeholders
     */
    public String toString(Player player)
    {
        StringBuilder sb = new StringBuilder();
        for (Object o : this)
        {
            if (o instanceof IPlaceholder && player != null)
                sb.append(((IPlaceholder) o).invoke(nagger, player));
            else sb.append(String.valueOf(o));
        }
        return sb.toString();
    }

    /**
     * Creates an array of StringBundles from an array of Strings<br/>
     * This method does not parse the strings, for that see {@link #parse(Nagger, String...)}<br/>
     * <i>Since: 1.0.0</i>
     *
     * @param nagger the Nagger to report errors to
     * @param array the array of strings to transform
     * @return
     */
    public static StringBundle[] fromArray(Nagger nagger, String... array) {
        StringBundle[] bundles = new StringBundle[array.length];
        for(int i = 0; i < array.length; i++)
            bundles[i] = new StringBundle(nagger, array[i]);
        return bundles;
    }

    /**
     * Parses an array of Strings into an array of StringBundles<br/>
     * <i>Since: 1.0.0</i>
     *
     * @param nagger the Nagger to report errors to
     * @param array the array of Strings to parse
     * @return the newly created array, containing parsed Strings
     */
    public static StringBundle[] parse(Nagger nagger, String... array) {
        StringBundle[] result = new StringBundle[array.length];
        for (int i = 0; i < array.length; i++)
            result[i] = parse(nagger, array[i]);
        return result;
    }

    /**
     * Parses a String into a StringBundle, this means:<br/>
     * Placeholders, unicode escapes and boxes (\x)<br/>
     * <i>Since: 1.0.0</i>
     *
     * @param nagger the nagger to report errors to
     * @param str the String to parse
     * @return a parsed String, as StringBundle
     */
    public static StringBundle parse(Nagger nagger, String str) {
        if (str == null) return null;

        StringBundle bundle = new StringBundle(nagger);
        StringBuilder builder = new StringBuilder();
        char[] array = str.toCharArray();
        int index = 0;
        boolean escape = false;
        loop:
        while (index < array.length) {
            char c = array[index];
            if (c == '\\') {
                if(!(escape = !escape)) //Escaped back-slash
                    builder.append(c);
            } else if (escape) {
                escape = false;
                switch (c) {
                    case 'u': //Unicode character
                        try {
                            char[] unicode = Arrays.copyOfRange(array, index + 1, index + 5);
                            index += 4;
                            builder.append((char) Integer.parseInt(new String(unicode), 16));
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            nagger.nag("Error on parsing unicode character in string " + str + ": "
                                    + "Expected number to have 4 digits!");
                            break loop;
                        } catch (NumberFormatException ex) {
                            nagger.nag("Error on parsing unicode character in string " + str + ": "
                                    + "Invalid characters (Allowed: 0-9 and A-F)!");
                            break loop;
                        }
                        break;
                    case '%': //Escaped placeholder character
                        builder.append(c);
                        break;
                    case 'x': case 'X':
                        builder.append(BOX);
                        break;
                }
            } else {
                switch(c) {
                    case '%': //Start of placeholder
                        if (index + 1 < array.length && array[index + 1] == c) {
                            index++;
                            builder.append(c);
                            break;
                        }
                        if (builder.length() > 0) {
                            bundle.add(builder.toString());
                            builder = new StringBuilder();
                        }
                        index++;
                        while (index < array.length && array[index] != '%')
                            builder.append(array[index++]);
                        if (index == array.length) {
                            nagger.nag("Placeholder " + builder + " wasn't closed off! Was it a mistake or "
                                    + "did you forget to escape the '%' symbol?");
                            break;
                        }
                        Placeholder placeholder = new Placeholder(builder.toString());
                        bundle.add(placeholder);
                        builder = new StringBuilder();
                        break;
                    default:
                        builder.append(c);
                        break;
                }
            }
            index++;
        }
        if(builder.length() > 0)
            bundle.add(builder.toString());
        return bundle;
    }
}
