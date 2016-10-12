package me.megamichiel.animationlib.command.exec;

public interface CommandExecutor {

    void onCommand(CommandContext ctx);

    default void tabComplete(TabCompleteContext ctx) {}
}
