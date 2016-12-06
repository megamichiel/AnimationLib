package me.megamichiel.animationlib.command;

import me.megamichiel.animationlib.command.annotation.CommandHandler;
import me.megamichiel.animationlib.command.exec.CommandContext;
import me.megamichiel.animationlib.command.exec.CommandExecutor;
import me.megamichiel.animationlib.command.exec.TabCompleteContext;

import java.util.Arrays;
import java.util.function.Predicate;

public class CommandInfo {

    private static final String[] EMPTY_ARRAY = new String[0];

    private final String name;
    private final CommandExecutor executor;
    private String[] aliases = EMPTY_ARRAY;
    private String desc, usage, permission, permissionMessage;

    public CommandInfo(String name, CommandExecutor executor) {
        this.name = name;
        this.executor = executor;
        usage = "/" + name;
        desc = "";
    }

    public CommandInfo(CommandHandler desc, CommandExecutor executor) {
        String[] names = desc.value();
        name = names[0];
        this.executor = executor;
        aliases = Arrays.copyOfRange(names, 0, names.length);
        this.desc = desc.desc();
        if (!desc.usage().isEmpty()) this.usage = desc.usage();
        else this.usage = "/" + name;
        if (!desc.perm().isEmpty()) this.permission = desc.perm();
        if (!desc.permMessage().isEmpty()) this.permissionMessage = desc.permMessage();
    }

    public String name() {
        return name;
    }

    public String[] aliases() {
        return aliases;
    }

    public CommandInfo aliases(String... aliases) {
        this.aliases = aliases == null ? EMPTY_ARRAY : aliases;
        return this;
    }

    public String desc() {
        return desc;
    }

    public CommandInfo desc(String desc) {
        this.desc = desc;
        return this;
    }

    public String usage() {
        return usage;
    }

    public CommandInfo usage(String usage) {
        this.usage = usage;
        return this;
    }

    public String permission() {
        return permission;
    }

    public CommandInfo permission(String permission) {
        this.permission = permission;
        return this;
    }

    public String permissionMessage() {
        return permissionMessage;
    }

    public CommandInfo permissionMessage(String permissionMessage) {
        this.permissionMessage = permissionMessage;
        return this;
    }

    public boolean testPermission(Predicate<String> checker) {
        return permission == null || checker.test(permission);
    }

    public void execute(CommandContext ctx) {
        executor.onCommand(ctx);
    }

    public void tabComplete(TabCompleteContext ctx) {
        executor.tabComplete(ctx);
    }
}
