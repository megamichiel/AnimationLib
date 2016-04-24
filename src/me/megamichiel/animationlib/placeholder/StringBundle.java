package me.megamichiel.animationlib.placeholder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import me.megamichiel.animationlib.Nagger;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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

    public boolean containsPlaceholders() {
        for (Object o : this)
            if (!(o instanceof String))
                return true;
        return false;
    }

    @Override
    public String invoke(Nagger nagger, Player player) {
        return toString(player);
    }

    public IPlaceholder<String> tryCache() {
        if (!containsPlaceholders()) return ConstantPlaceholder.of(toString(null));
        return this;
    }
    
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
    
    public static StringBundle[] fromArray(Nagger nagger, String... array) {
        StringBundle[] bundles = new StringBundle[array.length];
        for(int i = 0; i < array.length; i++)
            bundles[i] = new StringBundle(nagger, array[i]);
        return bundles;
    }
    
    public static StringBundle[] parse(Nagger nagger, String... array) {
        StringBundle[] result = new StringBundle[array.length];
        for (int i = 0; i < array.length; i++)
            result[i] = parse(nagger, array[i]);
        return result;
    }

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
