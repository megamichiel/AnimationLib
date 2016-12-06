package me.megamichiel.animationlib.bungee.category;

import me.megamichiel.animationlib.bungee.AnimLibPlugin;
import me.megamichiel.animationlib.bungee.RegisteredPlaceholder;

public class AnimLibCategory extends PlaceholderCategory {

    private final AnimLibPlugin plugin;

    protected AnimLibCategory(AnimLibPlugin plugin) {
        super("animlib");
        this.plugin = plugin;
    }

    @Override
    public RegisteredPlaceholder get(String value) {
        if (value.startsWith("formula_"))
            return plugin.getFormula(value.substring(8));
        return null;
    }
}
