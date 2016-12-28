package me.megamichiel.animationlib.util.pipeline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;

public class LongPipeline {
    
    private final PipelineContext ctx;
    private final List<LongPredicate> values = new ArrayList<>();

    LongPipeline(PipelineContext ctx) {
        this.ctx = ctx;
    }
    
    public void accept(long l) {
        for (Iterator<LongPredicate> it = values.iterator(); it.hasNext();)
            if (it.next().test(l))
                it.remove();
    }

    public LongPipeline filter(LongPredicate predicate) {
        LongPipeline pipeline = new LongPipeline(ctx);
        forEach(l -> {
            if (predicate.test(l)) pipeline.accept(l);
        });
        return pipeline;
    }

    public LongPipeline exclude(LongPredicate predicate) {
        LongPipeline pipeline = new LongPipeline(ctx);
        forEach(l -> {
            if (!predicate.test(l)) pipeline.accept(l);
        });
        return pipeline;
    }
    
    public LongPipeline map(LongUnaryOperator mapper) {
        LongPipeline pipeline = new LongPipeline(ctx);
        forEach(l -> pipeline.accept(mapper.applyAsLong(l)));
        return pipeline;
    }
    
    public <U> Pipeline<U> mapToObj(LongFunction<? extends U> mapper) {
        Pipeline<U> pipeline = new Pipeline<>(ctx);
        forEach(l -> pipeline.accept(mapper.apply(l)));
        return pipeline;
    }
    
    public IntPipeline mapToInt(LongToIntFunction mapper) {
        IntPipeline pipeline = new IntPipeline(ctx);
        forEach(l -> pipeline.accept(mapper.applyAsInt(l)));
        return pipeline;
    }
    
    public DoublePipeline mapToDouble(LongToDoubleFunction mapper) {
        return null;
    }
    
    public LongPipeline flatMap(LongFunction<? extends LongPipeline> mapper) {
        LongPipeline pipeline = new LongPipeline(ctx);
        forEach(l -> mapper.apply(l).forEach(pipeline::accept));
        return pipeline;
    }

    public LongPipeline acceptWhile(BooleanSupplier supplier) {
        LongPipeline pipeline = new LongPipeline(ctx);
        values.add(e -> {
            if (supplier.getAsBoolean()) {
                pipeline.accept(e);
                return false;
            }
            return true;
        });
        return pipeline;
    }

    public LongPipeline acceptUntil(BooleanSupplier supplier) {
        LongPipeline pipeline = new LongPipeline(ctx);
        values.add(e -> {
            if (supplier.getAsBoolean()) return true;
            pipeline.accept(e);
            return false;
        });
        return pipeline;
    }

    public LongPipeline acceptWhileBefore(long time) {
        return acceptWhile(() -> System.currentTimeMillis() < time);
    }

    public LongPipeline acceptUntil(long time) {
        return acceptUntil(() -> System.currentTimeMillis() >= time);
    }

    public LongPipeline skipUntil(BooleanSupplier supplier) {
        LongPipeline pipeline = new LongPipeline(ctx);
        values.add(e -> {
            if (supplier.getAsBoolean()) {
                forEach(pipeline::accept);
                return true;
            }
            return false;
        });
        return pipeline;
    }

    public LongPipeline skipUntil(long time) {
        return skipUntil(() -> System.currentTimeMillis() >= time);
    }

    public LongPipeline skipWhile(BooleanSupplier supplier) {
        LongPipeline pipeline = new LongPipeline(ctx);
        values.add(e -> {
            if (supplier.getAsBoolean()) return false;
            forEach(pipeline::accept);
            return true;
        });
        return pipeline;
    }

    public LongPipeline limit(long maxSize) {
        AtomicLong l = new AtomicLong(maxSize);
        return acceptUntil(() -> l.decrementAndGet() < 0);
    }

    public LongPipeline skip(long n) {
        AtomicLong l = new AtomicLong(n);
        return skipUntil(() -> l.decrementAndGet() < 0);
    }

    public LongPipeline peek(LongConsumer action) {
        forEach(action);
        return this;
    }

    public LongPipeline post(boolean async) {
        LongPipeline pipeline = new LongPipeline(ctx);
        forEach(l -> ctx.post(() -> pipeline.accept(l), async));
        return pipeline;
    }
    
    public void forEach(LongConsumer action) {
        values.add(i -> {
            action.accept(i);
            return false;
        });
    }
    
    public IntPipeline asIntPipeline() {
        IntPipeline pipeline = new IntPipeline(ctx);
        forEach(l -> pipeline.accept((int) l));
        return pipeline;
    }
    
    public DoublePipeline asDoublePipeline() {
        DoublePipeline pipeline = new DoublePipeline(ctx);
        forEach(pipeline::accept);
        return pipeline;
    }
    
    public Pipeline<Long> boxed() {
        return mapToObj(Long::new);
    }

    public void unregister() {
        ctx.onClose();
    }
}
