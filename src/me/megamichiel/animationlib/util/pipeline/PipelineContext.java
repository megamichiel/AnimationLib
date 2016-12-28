package me.megamichiel.animationlib.util.pipeline;

public interface PipelineContext {

    void onClose();
    void post(Runnable task, boolean async);
}
