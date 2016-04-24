package me.megamichiel.animationlib.animation;

import me.megamichiel.animationlib.Nagger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * A class that moves through multiple values using 'frames'
 *
 * @param <E> the type of this animatable
 */
public abstract class Animatable<E> extends ArrayList<E> {
    
    private static final long serialVersionUID = -7324365301382371283L;
    private int frame = 0;
    protected E defaultValue;

    public Animatable() {}
    
    public Animatable(Collection<? extends E> c)
    {
        super(c);
    }
    
    public Animatable(E[] elements)
    {
        super(Arrays.asList(elements));
    }

    /**
     * Returns the value at the current frame
     *
     * @return the value at the current frame, or {@link #defaultValue()} if this Animatable is empty
     */
    public E get()
    {
        return isEmpty() ? defaultValue() : get(frame);
    }

    /**
     * Returns the value retrieved by {@link #get(int)}, and moves to the next frame
     *
     * @return the value retrieved by {@link #get(int)}
     */
    public E next()
    {
        E current = get();
        if ((++frame) == size())
            frame = 0;
        return current;
    }

    /**
     * Converts a String to <i>E</i>
     *
     * @param nagger the {@link Nagger} to report warnings to
     * @param str the String to convert to <i>E</i>
     * @return the value created
     */
    protected E convert(Nagger nagger, String str)
    {
        return null;
    }

    /**
     * Returns the default value, which is retrieved by {@link #get(int)} if this Animatable is empty
     *
     * @return
     */
    protected E defaultValue()
    {
        return defaultValue;
    }

    /**
     * Loads this Animatable from <i>section</i> with key <i>key</i>
     *
     * @param nagger the {@link Nagger} to report warnings to
     * @param section the configuration section to get the value from
     * @param key the key to get the value from
     * @return true if this Animatable has loaded at least 1 value
     */
    public boolean load(Nagger nagger, ConfigurationSection section, String key)
    {
        return load(nagger, section, key, null);
    }

    /**
     * Loads this Animatable from <i>section</i> with key <i>key</i>
     *
     * @param nagger the {@link Nagger} to report warnings to
     * @param section the configuration section to get the value from
     * @param key the key to get the value from
     * @param defaultValue the value to retrieve if this Animatable is empty. Retrieved by the default {@link #defaultValue()}
     *
     * @return true if this Animatable has loaded at least 1 value
     */
    public boolean load(Nagger nagger, ConfigurationSection section, String key, E defaultValue)
    {
        this.defaultValue = defaultValue;
        if (section.isConfigurationSection(key))
        {
            ConfigurationSection sec = section.getConfigurationSection(key);
            String str;
            for (int num = 1; (str = sec.getString(String.valueOf(num))) != null; num++)
                add(convert(nagger, str));
            return true;
        }
        String value = section.getString(key);
        if (value != null)
        {
            add(convert(nagger, value));
            return true;
        }
        return false;
    }
}
