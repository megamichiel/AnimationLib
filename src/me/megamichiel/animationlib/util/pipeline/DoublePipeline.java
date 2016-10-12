package me.megamichiel.animationlib.util.pipeline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;

public class DoublePipeline {

    private final Runnable closer;
    private final List<DoublePredicate> values = new ArrayList<>();

    DoublePipeline(Runnable closer) {
        this.closer = closer;
    }

    public void accept(double d) {
        for (Iterator<DoublePredicate> it = values.iterator(); it.hasNext();)
            if (it.next().test(d))
                it.remove();
    }

    public DoublePipeline filter(DoublePredicate predicate) {
        DoublePipeline pipeline = new DoublePipeline(closer);
        forEach(d -> {
            if (predicate.test(d)) pipeline.accept(d);
        });
        return pipeline;
    }

    public DoublePipeline exclude(DoublePredicate predicate) {
        DoublePipeline pipeline = new DoublePipeline(closer);
        forEach(d -> {
            if (!predicate.test(d)) pipeline.accept(d);
        });
        return pipeline;
    }
    
    public DoublePipeline map(DoubleUnaryOperator mapper) {
        DoublePipeline pipeline = new DoublePipeline(closer);
        forEach(d -> pipeline.accept(mapper.applyAsDouble(d)));
        return pipeline;
    }
    
    public <U> Pipeline<U> mapToObj(DoubleFunction<? extends U> mapper) {
        Pipeline<U> pipeline = new Pipeline<>(closer);
        forEach(d -> pipeline.accept(mapper.apply(d)));
        return pipeline;
    }

    public IntPipeline mapToInt(DoubleToIntFunction mapper) {
        IntPipeline pipeline = new IntPipeline(closer);
        forEach(d -> pipeline.accept(mapper.applyAsInt(d)));
        return pipeline;
    }
    
    public LongPipeline mapToLong(DoubleToLongFunction mapper) {
        LongPipeline pipeline = new LongPipeline(closer);
        forEach(d -> pipeline.accept(mapper.applyAsLong(d)));
        return pipeline;
    }
    
    public DoublePipeline flatMap(DoubleFunction<? extends DoublePipeline> mapper) {
        DoublePipeline pipeline = new DoublePipeline(closer);
        forEach(d -> mapper.apply(d).forEach(pipeline::accept));
        return pipeline;
    }

    public DoublePipeline acceptWhile(BooleanSupplier supplier) {
        DoublePipeline pipeline = new DoublePipeline(closer);
        values.add(e -> {
            if (supplier.getAsBoolean()) {
                pipeline.accept(e);
                return false;
            }
            return true;
        });
        return pipeline;
    }

    public DoublePipeline acceptUntil(BooleanSupplier supplier) {
        DoublePipeline pipeline = new DoublePipeline(closer);
        values.add(e -> {
            if (supplier.getAsBoolean()) return true;
            pipeline.accept(e);
            return false;
        });
        return pipeline;
    }

    public DoublePipeline acceptWhileBefore(long time) {
        return acceptWhile(() -> System.currentTimeMillis() < time);
    }

    public DoublePipeline acceptUntil(long time) {
        return acceptUntil(() -> System.currentTimeMillis() >= time);
    }

    public DoublePipeline skipUntil(BooleanSupplier supplier) {
        DoublePipeline pipeline = new DoublePipeline(closer);
        values.add(e -> {
            if (supplier.getAsBoolean()) {
                forEach(pipeline::accept);
                return true;
            }
            return false;
        });
        return pipeline;
    }

    public DoublePipeline skipUntil(long time) {
        return skipUntil(() -> System.currentTimeMillis() >= time);
    }

    public DoublePipeline skipWhile(BooleanSupplier supplier) {
        DoublePipeline pipeline = new DoublePipeline(closer);
        values.add(e -> {
            if (supplier.getAsBoolean()) return false;
            forEach(pipeline::accept);
            return true;
        });
        return pipeline;
    }

    public DoublePipeline limit(long maxSize) {
        AtomicLong l = new AtomicLong(maxSize);
        return acceptUntil(() -> l.decrementAndGet() < 0);
    }

    public DoublePipeline skip(long n) {
        AtomicLong l = new AtomicLong(n);
        return skipUntil(() -> l.decrementAndGet() < 0);
    }

    public DoublePipeline peek(DoubleConsumer action) {
        forEach(action);
        return this;
    }
    
    public void forEach(DoubleConsumer action) {
        values.add(d -> {
            action.accept(d);
            return false;
        });
    }

    public IntPipeline asIntPipeline() {
        IntPipeline pipeline = new IntPipeline(closer);
        forEach(d -> pipeline.accept((int) d));
        return pipeline;
    }
    
    public LongPipeline asLongPipeline() {
        LongPipeline pipeline = new LongPipeline(closer);
        forEach(d -> pipeline.accept((long) d));
        return pipeline;
    }
    
    public Pipeline<Double> boxed() {
        return mapToObj(Double::new);
    }

    public void unregister() {
        closer.run();
    }
}
