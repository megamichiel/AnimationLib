package me.megamichiel.animationlib.command;

import me.megamichiel.animationlib.command.CommandAPI;
import me.megamichiel.animationlib.util.Subscription;

public class CommandSubscription<C> implements Subscription {

    private final CommandAPI<?, C> api;
    private final C command;

    protected boolean unsubscribed;

    public CommandSubscription(CommandAPI<?, C> api, C command) {
        this.api = api;
        this.command = command;
    }

    @Override
    public void unsubscribe() {
        api.deleteCommands((s, cmd) -> cmd == command);
        unsubscribed = true;
    }

    @Override
    public boolean isUnsubscribed() {
        return unsubscribed;
    }

    public C getCommand() {
        return command;
    }
}
