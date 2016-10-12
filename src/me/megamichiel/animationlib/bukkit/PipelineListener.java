package me.megamichiel.animationlib.bukkit;

import me.megamichiel.animationlib.util.pipeline.Pipeline;
import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import java.lang.reflect.Method;

public class PipelineListener<E extends Event> implements EventExecutor, Listener {

    private static HandlerList getHandlerList(Class<? extends Event> c) {
        try {
            Method m = c.getDeclaredMethod("getHandlerList");
            m.setAccessible(true);
            return (HandlerList) m.invoke(null);
        } catch (Exception ex) {
            Class<?> parent = c.getSuperclass();
            if (Event.class.isAssignableFrom(parent))
                return getHandlerList(parent.asSubclass(Event.class));
            return null;
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
    private final Pipeline<E> head;
    private boolean ignoreCancelled = false;

    private PipelineListener(Class<E> type, EventPriority priority, Plugin plugin) {
        HandlerList handlers = getHandlerList(type);
        if (handlers == null)
            throw new IllegalArgumentException(type.getName() + " has no getHandlerList() method!");
        this.type = type;
        Handler handler = new Handler(priority, plugin);
        handlers.register(handler);
        head = new Pipeline<>(() -> handlers.unregister(handler));
    }

    public PipelineListener ignoreCancelled(boolean ignoreCancelled) {
        this.ignoreCancelled = ignoreCancelled;
        return this;
    }

    public PipelineListener ignoreCancelled() {
        return ignoreCancelled(true);
    }

    public boolean isIgnoringCancelled() {
        return ignoreCancelled;
    }

    @Override
    public void execute(Listener listener, Event event) throws EventException {
        if (type.isInstance(event))
            head.accept(type.cast(event));
    }

    private class Handler extends RegisteredListener {

        private Handler(EventPriority priority, Plugin plugin) {
            super(PipelineListener.this, PipelineListener.this,
                    priority, plugin, false);
        }

        @Override
        public boolean isIgnoringCancelled() {
            return ignoreCancelled;
        }
    }
}
