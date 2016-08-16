package me.megamichiel.animationlib.bungee;

import com.google.common.collect.Multimap;
import me.megamichiel.animationlib.LazyValue;
import me.megamichiel.animationlib.util.pipeline.Pipeline;
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

public class PipelineListener<E extends Event> implements Listener {

    private static final LazyValue<EventBus> eventBus = LazyValue.unsafe(() -> {
        Field field = PluginManager.class.getDeclaredField("eventBus");
        field.setAccessible(true);
        return (EventBus) field.get(BungeeCord.getInstance().getPluginManager());
    });
    private static final LazyValue<Lock> lock = LazyValue.unsafe(() -> {
        Field field = EventBus.class.getDeclaredField("lock");
        field.setAccessible(true);
        return (Lock) field.get(eventBus.get());
    });
    private static final LazyValue<Map<Class<?>, Map<Byte, Map<Object, Method[]>>>> byListenerAndPriority = LazyValue.unsafe(() -> {
        Field field = EventBus.class.getDeclaredField("byListenerAndPriority");
        field.setAccessible(true);
        return (Map<Class<?>, Map<Byte, Map<Object, Method[]>>>) field.get(eventBus.get());
    });
    private static final LazyValue<Method> callEvent = LazyValue.unsafe(() -> {
        Method m = PipelineListener.class.getDeclaredMethod("callEvent", Event.class);
        m.setAccessible(true);
        return m;
    });
    private static final LazyValue<Consumer<Class<?>>> bakeHandlers = LazyValue.unsafe(() -> {
        Method m = EventBus.class.getDeclaredMethod("bakeHandlers", Class.class);
        m.setAccessible(true);
        return type -> {
            try {
                m.invoke(eventBus.get(), type);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    });
    private static final LazyValue<Multimap<Plugin, Listener>> listenersByPlugin = LazyValue.unsafe(() -> {
        Field field = PluginManager.class.getDeclaredField("listenersByPlugin");
        field.setAccessible(true);
        return (Multimap<Plugin, Listener>) field.get(BungeeCord.getInstance().getPluginManager());
    });

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
    private final Pipeline<E> pipeline;

    private PipelineListener(Class<E> type, byte priority, Plugin plugin) {
        this.type = type;
        pipeline = new Pipeline<>(() -> {
            lock.get().lock();
            try {
                Map<Byte, Map<Object, Method[]>> prioritiesMap = byListenerAndPriority.get().get(type);
                if (prioritiesMap != null) {
                    Map<Object, Method[]> currentPriorityMap = prioritiesMap.get(priority);
                    if (currentPriorityMap != null) {
                        currentPriorityMap.remove(this);
                        if (currentPriorityMap.isEmpty())
                            prioritiesMap.remove(priority);
                    }
                    if (prioritiesMap.isEmpty())
                        byListenerAndPriority.get().remove(type);
                }
            } finally {
                lock.get().unlock();
            }
            listenersByPlugin.get().values().remove(this);
        });
        lock.get().lock();
        try {
            Map<Byte, Map<Object, Method[]>> prioritiesMap = byListenerAndPriority.get().get(type);
            if (prioritiesMap == null) {
                prioritiesMap = new HashMap<>();
                byListenerAndPriority.get().put(type, prioritiesMap);
            }
            Map<Object, Method[]> currentPriorityMap = prioritiesMap.get(priority);
            if (currentPriorityMap == null) {
                currentPriorityMap = new HashMap<>();
                prioritiesMap.put(priority, currentPriorityMap);
            }
            currentPriorityMap.put(this, new Method[] { callEvent.get() });
            bakeHandlers.get().accept(type);
            listenersByPlugin.get().put(plugin, this);
        } finally {
            lock.get().unlock();
        }
    }

    private void callEvent(Event event) {
        if (type.isInstance(event))
            pipeline.accept(type.cast(event));
    }
}
