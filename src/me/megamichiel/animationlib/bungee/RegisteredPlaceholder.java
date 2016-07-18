package me.megamichiel.animationlib.bungee;

import me.megamichiel.animationlib.Nagger;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public interface RegisteredPlaceholder {

    String invoke(Nagger nagger, ProxiedPlayer player);
}
