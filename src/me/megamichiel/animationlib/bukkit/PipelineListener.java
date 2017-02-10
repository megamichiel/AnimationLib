package me.megamichiel.animationlib.bukkit;

import me.megamichiel.animationlib.util.pipeline.Pipeline;
import me.megamichiel.animationlib.util.pipeline.PipelineContext;
import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import java.lang.reflect.Method;

public class PipelineListener<E extends Event>
        implements EventExecutor, Listener, PipelineContext {

    private static HandlerList getHandlerList(Class<? extends Event> c) {
        try {
            Method m = c.getDeclaredMethod("getHandlerList");
            m.setAccessible(true);
            return (HandlerList) m.invoke(null);
        } catch (Exception ex) {
            try {
                return getHandlerList(c.getSuperclass().asSubclass(Event.class));
            } catch (ClassCastException ex2) {
                return null;
            }
        }
    }

    public static <E extends Event> Pipeline<E> newPipeline(Class<E> type,
                                                            EventPriority priority,
                                                            Plugin plugin) {
        return new PipelineListener<>(type, priority, plugin).head;
    }

    public static <E extends Event> Pipeline<E> newPipeline(Class<E> type, Plugin plugin) {
        return newPipeline(type, EventPriority.NORMAL, plugin);
    }

    private final Class<E> type;
    private final HandlerList handlers;
    private final Handler handler;

    private final Plugin plugin;
    private final Pipeline<E> head;

    private PipelineListener(Class<E> type, EventPriority priority, Plugin plugin) {
        HandlerList handlers = getHandlerList(type);
        if (handlers == null) {
            throw new IllegalArgumentException(type.getName() + " has no getHandlerList() method!");
        }
        this.handlers = handlers;
        this.type = type;
        this.plugin = plugin;
        handlers.register(handler = new Handler(priority, plugin));
        head = new Pipeline<>(this);
    }

    @Override
    public void execute(Listener listener, Event event) throws EventException {
        if (type.isInstance(event)) {
            head.accept(type.cast(event));
        }
    }

    @Override
    public void onClose() {
        handlers.unregister(handler);
    }

    @Override
    public void post(Runnable task, boolean async) {
        if (async) plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
        else plugin.getServer().getScheduler().runTask(plugin, task);
    }

    private class Handler extends RegisteredListener {

        private Handler(EventPriority priority, Plugin plugin) {
            super(PipelineListener.this, PipelineListener.this, priority, plugin, false);
        }
    }
}
