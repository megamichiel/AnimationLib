package me.megamichiel.animationlib.util.pipeline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;

public class Pipeline<E> {

    private final Runnable closer;
    private final List<Predicate<? super E>> values = new ArrayList<>();

    public Pipeline(Runnable closer) {
        this.closer = closer;
    }

    public void accept(E value) {
        for (Iterator<Predicate<? super E>> it = values.iterator(); it.hasNext(); )
            if (it.next().test(value))
                it.remove();
    }
    
    public Pipeline<E> filter(Predicate<? super E> predicate) {
        Pipeline<E> pipeline = new Pipeline<>(closer);
        forEach(e -> {
            if (predicate.test(e)) pipeline.accept(e);
        });
        return pipeline;
    }

    public Pipeline<E> nonNull() {
        return filter(Objects::nonNull);
    }

    public Pipeline<E> exclude(Predicate<? super E> predicate) {
        Pipeline<E> pipeline = new Pipeline<>(closer);
        forEach(e -> {
            if (!predicate.test(e)) pipeline.accept(e);
        });
        return pipeline;
    }
    
    public <R> Pipeline<R> map(Function<? super E, ? extends R> mapper) {
        Pipeline<R> pipeline = new Pipeline<>(closer);
        forEach(e -> pipeline.accept(mapper.apply(e)));
        return pipeline;
    }
    
    public IntPipeline mapToInt(ToIntFunction<? super E> mapper) {
        IntPipeline pipeline = new IntPipeline(closer);
        forEach(e -> pipeline.accept(mapper.applyAsInt(e)));
        return pipeline;
    }
    
    public LongPipeline mapToLong(ToLongFunction<? super E> mapper) {
        LongPipeline pipeline = new LongPipeline(closer);
        forEach(e -> pipeline.accept(mapper.applyAsLong(e)));
        return pipeline;
    }
    
    public DoublePipeline mapToDouble(ToDoubleFunction<? super E> mapper) {
        DoublePipeline pipeline = new DoublePipeline(closer);
        forEach(e -> pipeline.accept(mapper.applyAsDouble(e)));
        return pipeline;
    }
    
    public <R> Pipeline<R> flatMap(Function<? super E, ? extends Pipeline<? extends R>> mapper) {
        Pipeline<R> pipeline = new Pipeline<>(closer);
        forEach(e -> mapper.apply(e).forEach(pipeline::accept));
        return pipeline;
    }
    
    public IntPipeline flatMapToInt(Function<? super E, ? extends IntPipeline> mapper) {
        IntPipeline pipeline = new IntPipeline(closer);
        forEach(e -> mapper.apply(e).forEach(pipeline::accept));
        return pipeline;
    }
    
    public LongPipeline flatMapToLong(Function<? super E, ? extends LongPipeline> mapper) {
        LongPipeline pipeline = new LongPipeline(closer);
        forEach(e -> mapper.apply(e).forEach(pipeline::accept));
        return pipeline;
    }
    
    public DoublePipeline flatMapToDouble(Function<? super E, ? extends DoublePipeline> mapper) {
        DoublePipeline pipeline = new DoublePipeline(closer);
        forEach(e -> mapper.apply(e).forEach(pipeline::accept));
        return pipeline;
    }
    
    public Pipeline<E> acceptWhile(BooleanSupplier supplier) {
        Pipeline<E> pipeline = new Pipeline<>(closer);
        values.add(e -> {
            if (supplier.getAsBoolean()) {
                pipeline.accept(e);
                return false;
            }
            return true;
        });
        return pipeline;
    }

    public Pipeline<E> acceptUntil(BooleanSupplier supplier) {
        Pipeline<E> pipeline = new Pipeline<>(closer);
        values.add(e -> {
            if (supplier.getAsBoolean()) return true;
            pipeline.accept(e);
            return false;
        });
        return pipeline;
    }

    public Pipeline<E> acceptWhileBefore(long time) {
        return acceptWhile(() -> System.currentTimeMillis() < time);
    }

    public Pipeline<E> acceptUntil(long time) {
        return acceptUntil(() -> System.currentTimeMillis() >= time);
    }

    public Pipeline<E> skipUntil(BooleanSupplier supplier) {
        Pipeline<E> pipeline = new Pipeline<>(closer);
        values.add(e -> {
            if (supplier.getAsBoolean()) {
                forEach(pipeline::accept);
                return true;
            }
            return false;
        });
        return pipeline;
    }

    public Pipeline<E> skipUntil(long time) {
        return skipUntil(() -> System.currentTimeMillis() >= time);
    }

    public Pipeline<E> skipWhile(BooleanSupplier supplier) {
        Pipeline<E> pipeline = new Pipeline<>(closer);
        values.add(e -> {
            if (supplier.getAsBoolean()) return false;
            forEach(pipeline::accept);
            return true;
        });
        return pipeline;
    }
    
    public Pipeline<E> limit(long maxSize) {
        AtomicLong l = new AtomicLong(maxSize);
        return acceptUntil(() -> l.decrementAndGet() < 0);
    }
    
    public Pipeline<E> skip(long n) {
        AtomicLong l = new AtomicLong(n);
        return skipUntil(() -> l.decrementAndGet() < 0);
    }
    
    public void forEach(Consumer<? super E> action) {
        values.add(e -> {
            action.accept(e);
            return true;
        });
    }

    public Pipeline<E> peek(Consumer<? super E> action) {
        forEach(action);
        return this;
    }
    
    public void unregister() {
        closer.run();
    }
}
