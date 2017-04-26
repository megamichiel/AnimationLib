package me.megamichiel.animationlib.util.pipeline;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;

public class Pipeline<E> extends AbstractPipeline<Predicate<? super E>> {

    public Pipeline(PipelineContext ctx) {
        super(ctx);
    }

    public void accept(E value) {
        _accept(p -> p.test(value));
    }
    
    public Pipeline<E> filter(Predicate<? super E> predicate) {
        Pipeline<E> pipeline = new Pipeline<>(ctx);
        forEach(e -> {
            if (predicate.test(e)) pipeline.accept(e);
        });
        return pipeline;
    }

    public Pipeline<E> nonNull() {
        return filter(Objects::nonNull);
    }

    public Pipeline<E> exclude(Predicate<? super E> predicate) {
        Pipeline<E> pipeline = new Pipeline<>(ctx);
        forEach(e -> {
            if (!predicate.test(e)) pipeline.accept(e);
        });
        return pipeline;
    }
    
    public <R> Pipeline<R> map(Function<? super E, ? extends R> mapper) {
        Pipeline<R> pipeline = new Pipeline<>(ctx);
        forEach(e -> pipeline.accept(mapper.apply(e)));
        return pipeline;
    }

    public <R> Pipeline<R> cast(Class<R> type) {
        Pipeline<R> pipeline = new Pipeline<>(ctx);
        forEach(e -> {
            if (type.isInstance(e)) pipeline.accept(type.cast(e));
        });
        return pipeline;
    }
    
    public IntPipeline mapToInt(ToIntFunction<? super E> mapper) {
        IntPipeline pipeline = new IntPipeline(ctx);
        forEach(e -> pipeline.accept(mapper.applyAsInt(e)));
        return pipeline;
    }
    
    public LongPipeline mapToLong(ToLongFunction<? super E> mapper) {
        LongPipeline pipeline = new LongPipeline(ctx);
        forEach(e -> pipeline.accept(mapper.applyAsLong(e)));
        return pipeline;
    }
    
    public DoublePipeline mapToDouble(ToDoubleFunction<? super E> mapper) {
        DoublePipeline pipeline = new DoublePipeline(ctx);
        forEach(e -> pipeline.accept(mapper.applyAsDouble(e)));
        return pipeline;
    }
    
    public <R> Pipeline<R> flatMap(Function<? super E, ? extends Pipeline<? extends R>> mapper) {
        Pipeline<R> pipeline = new Pipeline<>(ctx);
        forEach(e -> mapper.apply(e).forEach(pipeline::accept));
        return pipeline;
    }
    
    public IntPipeline flatMapToInt(Function<? super E, ? extends IntPipeline> mapper) {
        IntPipeline pipeline = new IntPipeline(ctx);
        forEach(e -> mapper.apply(e).forEach(pipeline::accept));
        return pipeline;
    }
    
    public LongPipeline flatMapToLong(Function<? super E, ? extends LongPipeline> mapper) {
        LongPipeline pipeline = new LongPipeline(ctx);
        forEach(e -> mapper.apply(e).forEach(pipeline::accept));
        return pipeline;
    }
    
    public DoublePipeline flatMapToDouble(Function<? super E, ? extends DoublePipeline> mapper) {
        DoublePipeline pipeline = new DoublePipeline(ctx);
        forEach(e -> mapper.apply(e).forEach(pipeline::accept));
        return pipeline;
    }
    
    public Pipeline<E> acceptWhile(Predicate<E> predicate) {
        Pipeline<E> pipeline = new Pipeline<>(ctx);
        add(e -> {
            if (predicate.test(e)) {
                pipeline.accept(e);
                return false;
            }
            return true;
        });
        return pipeline;
    }

    public Pipeline<E> acceptUntil(Predicate<E> predicate) {
        Pipeline<E> pipeline = new Pipeline<>(ctx);
        add(e -> {
            if (predicate.test(e)) return true;
            pipeline.accept(e);
            return false;
        });
        return pipeline;
    }

    public Pipeline<E> acceptWhileBefore(long time) {
        return acceptWhile(e -> System.currentTimeMillis() < time);
    }

    public Pipeline<E> acceptUntil(long time) {
        return acceptUntil(e -> System.currentTimeMillis() >= time);
    }

    public Pipeline<E> skipUntil(Predicate<E> predicate) {
        Pipeline<E> pipeline = new Pipeline<>(ctx);
        add(e -> {
            if (predicate.test(e)) {
                forEach(pipeline::accept);
                return true;
            }
            return false;
        });
        return pipeline;
    }

    public Pipeline<E> skipUntil(long time) {
        return skipUntil(e -> System.currentTimeMillis() >= time);
    }

    public Pipeline<E> skipWhile(Predicate<E> predicate) {
        Pipeline<E> pipeline = new Pipeline<>(ctx);
        add(e -> {
            if (predicate.test(e)) return false;
            forEach(pipeline::accept);
            return true;
        });
        return pipeline;
    }
    
    public Pipeline<E> limit(long maxSize) {
        AtomicLong l = new AtomicLong(maxSize);
        return acceptUntil(e -> l.decrementAndGet() < 0);
    }
    
    public Pipeline<E> skip(long n) {
        AtomicLong l = new AtomicLong(n);
        return skipUntil(e -> l.decrementAndGet() < 0);
    }

    public Pipeline<E> post(boolean async) {
        Pipeline<E> pipeline = new Pipeline<>(ctx);
        forEach(e -> ctx.post(() -> pipeline.accept(e), async));
        return pipeline;
    }
    
    public void forEach(Consumer<? super E> action) {
        add(e -> {
            action.accept(e);
            return false;
        });
    }

    public Pipeline<E> peek(Consumer<? super E> action) {
        forEach(action);
        return this;
    }
}
