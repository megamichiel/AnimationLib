package me.megamichiel.animationlib.bungee;

import com.google.common.collect.Multimap;
import me.megamichiel.animationlib.util.pipeline.Pipeline;
import me.megamichiel.animationlib.util.pipeline.PipelineContext;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.event.EventBus;
import net.md_5.bungee.event.EventPriority;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public class PipelineListener<E extends Event> implements Listener, PipelineContext {

    private final Lock lock;
    private final Map<Class<?>, Map<Byte, Map<Object, Method[]>>> byListenerAndPriority;
    private final Method callEvent;
    private final Consumer<Class<?>> bakeHandlers;
    private final Multimap<Plugin, Listener> listenersByPlugin;
    
    {
        Lock lock;
        Map<Class<?>, Map<Byte, Map<Object, Method[]>>> map;
        Method method;
        Consumer<Class<?>> consumer;
        Multimap<Plugin, Listener> mmap;
        try {
            Field field;
            (field = PluginManager.class.getDeclaredField("eventBus")).setAccessible(true);
            EventBus bus = (EventBus) field.get(BungeeCord.getInstance().getPluginManager());
            
            (field = EventBus.class.getDeclaredField("lock")).setAccessible(true);
            lock = (Lock) field.get(bus);
            
            (field = EventBus.class.getDeclaredField("byListenerAndPriority")).setAccessible(true);
            map = (Map<Class<?>, Map<Byte, Map<Object, Method[]>>>) field.get(bus);
            
            (method = PipelineListener.class.getDeclaredMethod("callEvent", Event.class)).setAccessible(true);

            Method m = EventBus.class.getDeclaredMethod("bakeHandlers", Class.class);
            m.setAccessible(true);
            consumer = type -> {
                try {
                    m.invoke(bus, type);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            };
            
            (field = PluginManager.class.getDeclaredField("listenersByPlugin")).setAccessible(true);
            mmap = (Multimap<Plugin, Listener>) field.get(BungeeCord.getInstance().getPluginManager());
        } catch (Exception ex) {
            lock = null;
            map = null;
            method = null;
            consumer = null;
            mmap = null;
        }
        this.lock = lock;
        byListenerAndPriority = map;
        callEvent = method;
        bakeHandlers = consumer;
        listenersByPlugin = mmap;
    }

    public static <E extends Event> Pipeline<E> newPipeline(Class<E> type,
                                                            Plugin plugin) {
        return newPipeline(type, EventPriority.NORMAL, plugin);
    }

    public static <E extends Event> Pipeline<E> newPipeline(Class<E> type,
                                                            byte priority,
                                                            Plugin plugin) {
        return new PipelineListener<>(type, priority, plugin).pipeline;
    }

    private final Class<E> type;
    private final byte priority;

    private final Plugin plugin;
    private final Pipeline<E> pipeline;

    private PipelineListener(Class<E> type, byte priority, Plugin plugin) {
        this.type = type;
        this.priority = priority;
        this.plugin = plugin;
        pipeline = new Pipeline<>(this);
        lock.lock();
        try {
            byListenerAndPriority.computeIfAbsent(type, k -> new HashMap<>())
                    .computeIfAbsent(priority, k -> new HashMap<>())
                    .put(this, new Method[] { callEvent });
            bakeHandlers.accept(type);
            listenersByPlugin.put(plugin, this);
        } finally {
            lock.unlock();
        }
    }

    private void callEvent(Event event) {
        if (type.isInstance(event))
            pipeline.accept(type.cast(event));
    }

    @Override
    public void onClose() {
        lock.lock();
        try {
            Map<Byte, Map<Object, Method[]>> prioritiesMap = byListenerAndPriority.get(type);
            if (prioritiesMap != null) {
                Map<Object, Method[]> currentPriorityMap = prioritiesMap.get(priority);
                if (currentPriorityMap != null) {
                    currentPriorityMap.remove(this);
                    if (currentPriorityMap.isEmpty())
                        prioritiesMap.remove(priority);
                }
                if (prioritiesMap.isEmpty())
                    byListenerAndPriority.remove(type);
            }
        } finally {
            lock.unlock();
        }
        listenersByPlugin.values().remove(this);
    }

    @Override
    public void post(Runnable task, boolean async) {
        plugin.getProxy().getScheduler().runAsync(plugin, task);
    }
}
