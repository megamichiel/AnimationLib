package me.megamichiel.animationlib.bukkit;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.ctx.ConcurrentContext;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class BukkitConcurrentContext extends ConcurrentContext {

    private final Plugin plugin;

    public BukkitConcurrentContext(Plugin plugin, Nagger nagger, int updatesPerTick) {
        super(nagger, updatesPerTick);
        this.plugin = plugin;
        PipelineListener.newPipeline(PlayerQuitEvent.class, plugin)
                .map(PlayerQuitEvent::getPlayer).forEach(this::playerQuit);
    }

    public void schedule(long delay, long period) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, delay, period);
    }
}
