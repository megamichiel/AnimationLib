package me.megamichiel.animationlib.util.pipeline;

import java.util.function.Predicate;

public class AbstractPipeline<P> {

    protected final PipelineContext ctx;
    private Entry<P> head, tail;

    protected AbstractPipeline(PipelineContext ctx) {
        this.ctx = ctx;
    }

    protected void add(P predicate) {
        Entry<P> entry = new Entry<>(predicate);
        if (tail != null) {
            tail = tail.next = entry;
        } else {
            head = tail = entry;
        }
    }

    protected void _accept(Predicate<P> predicate) {
        for (Entry<P> entry = head, prev = null; entry != null; entry = (prev = entry).next)
            if (predicate.test(entry.value) &&
                    (prev != null ? (prev.next = entry.next) : (head = entry.next)) == null)
                tail = prev;
    }

    public void unregister() {
        if (ctx != null) ctx.onClose();
    }

    private static class Entry<T> {

        final T value;
        Entry<T> next;

        Entry(T value) {
            this.value = value;
        }
    }
}
