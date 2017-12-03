package me.megamichiel.animationlib.command.exec;

import me.megamichiel.animationlib.command.BaseCommandAPI;

public class CommandContext<S, C> {

    private final BaseCommandAPI<S, C> api;

    private final S sender;
    private final C command;
    private final String label;
    private final String[] args;

    public CommandContext(BaseCommandAPI<S, C> api, S sender,
                          C command, String label, String[] args) {
        this.api = api;
        this.sender = sender;
        this.command = command;
        this.label = label;
        this.args = args;
    }

    public S getSender() {
        return sender;
    }

    public void sendMessage(String message) {
        api.sendMessage(sender, message);
    }

    public C getCommand() {
        return command;
    }

    public String getLabel() {
        return label;
    }

    public String[] getArgs() {
        return args;
    }

    public int length() {
        return args.length;
    }

    public String getArg(int index) {
        return index < args.length ? args[index] : null;
    }
}
