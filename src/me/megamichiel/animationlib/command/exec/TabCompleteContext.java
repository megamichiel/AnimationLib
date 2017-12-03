package me.megamichiel.animationlib.command.exec;

import me.megamichiel.animationlib.command.BaseCommandAPI;

import java.util.ArrayList;
import java.util.List;

public class TabCompleteContext<S, C> extends CommandContext<S, C> {

    private List<String> completions = new ArrayList<>();

    public TabCompleteContext(BaseCommandAPI<S, C> api, S sender, C command, String label, String[] args) {
        super(api, sender, command, label, args);
    }

    public List<String> getCompletions() {
        return completions;
    }

    public void setCompletions(List<String> completions) {
        this.completions = completions;
    }
}
