package me.megamichiel.animationlib.placeholder.ctx;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.PlaceholderContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentContext implements Runnable, PlaceholderContext {

    private final Nagger nagger;
    private final int updatesPerTick;
    private int entryIndex = 0;

    // {Player::{Identifier::Entry}}
    private final Map<Object, Map<String, Entry>> values = new ConcurrentHashMap<>();
    private final List<Entry> entries = new ArrayList<>();

    protected ConcurrentContext(Nagger nagger, int updatesPerTick) {
        this.nagger = nagger;
        this.updatesPerTick = updatesPerTick;
    }

    protected void playerQuit(Object player) {
        Map<String, Entry> values = this.values.remove(player);
        if (values != null) entries.removeAll(values.values());
    }

    @Override
    public void run() {
        int index = entryIndex, size = entries.size();
        if (index == -1) {
            entries.forEach(Entry::refresh);
            return;
        }
        for (int i = updatesPerTick; i-- != 0;) {
            if (index == size) index = 0; // Start over again
            if (index == entryIndex) return; // Stop when we've refreshed em all
            entries.get(index++).refresh();
        }
        entryIndex = index;
    }

    @Override
    public Object get(Object who, String identifier) {
        Map<String, Entry> map = values.get(who);
        return map == null ? null : map.get(identifier);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T invoke(Object who, String identifier, IPlaceholder<T> placeholder) {
        return (T) values.computeIfAbsent(who, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(identifier, k -> new Entry(who, placeholder)).value;
    }

    private class Entry {

        private final Object player;
        private final IPlaceholder<?> placeholder;
        private Object value;

        private Entry(Object player, IPlaceholder<?> placeholder) {
            this.player = player;
            this.placeholder = placeholder;
            value = placeholder.invoke(nagger, player);
            entries.add(this);
        }

        void refresh() {
            value = placeholder.invoke(nagger, player);
        }
    }
}
