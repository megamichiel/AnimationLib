package me.megamichiel.animationlib.util.pipeline;

public interface PipelineContext {

    boolean ASYNC = true, SYNC = false;

    void onClose();
    void post(Runnable task, boolean async);
}
