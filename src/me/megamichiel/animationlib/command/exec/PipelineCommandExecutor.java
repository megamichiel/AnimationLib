package me.megamichiel.animationlib.command.exec;

import me.megamichiel.animationlib.util.pipeline.Pipeline;

public class PipelineCommandExecutor implements CommandExecutor {

    private final Pipeline<CommandContext> execute = new Pipeline<>(null);
    private final Pipeline<TabCompleteContext> tabComplete = new Pipeline<>(null);

    public Pipeline<CommandContext> execute() {
        return execute;
    }

    public Pipeline<TabCompleteContext> tabComplete() {
        return tabComplete;
    }

    @Override
    public void onCommand(CommandContext ctx) {
        execute.accept(ctx);
    }

    @Override
    public void tabComplete(TabCompleteContext ctx) {
        tabComplete.accept(ctx);
    }
}
