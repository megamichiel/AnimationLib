package me.megamichiel.animationlib.bungee;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.bukkit.*;
import me.megamichiel.animationlib.placeholder.ctx.ConcurrentContext;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public class BungeeConcurrentContext extends ConcurrentContext {

    private final Plugin plugin;

    protected BungeeConcurrentContext(Plugin plugin, Nagger nagger, int updatesPerTick) {
        super(nagger, updatesPerTick);
        this.plugin = plugin;
        PipelineListener.newPipeline(PlayerDisconnectEvent.class, plugin)
                .map(PlayerDisconnectEvent::getPlayer).forEach(this::playerQuit);
    }

    public void schedule(long delay, long period) {
        plugin.getProxy().getScheduler().schedule(plugin, this, delay * 50, period * 50, TimeUnit.MILLISECONDS);
    }
}
