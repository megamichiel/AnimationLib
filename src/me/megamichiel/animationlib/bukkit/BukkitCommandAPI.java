package me.megamichiel.animationlib.bukkit;

import me.megamichiel.animationlib.LazyValue;
import me.megamichiel.animationlib.command.BaseCommandAPI;
import me.megamichiel.animationlib.command.CommandInfo;
import me.megamichiel.animationlib.command.CommandSubscription;
import me.megamichiel.animationlib.command.exec.CommandContext;
import me.megamichiel.animationlib.command.exec.TabCompleteContext;
import me.megamichiel.animationlib.util.ReflectClass;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.help.GenericCommandHelpTopic;
import org.bukkit.help.HelpMap;
import org.bukkit.help.HelpTopic;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.bukkit.Bukkit.getServer;

public class BukkitCommandAPI extends BaseCommandAPI<CommandSender, Command> {

    public static BukkitCommandAPI getInstance() {
        return (BukkitCommandAPI) BaseCommandAPI.<CommandSender, Command>getInstance();
    }

    private static final Supplier<CommandMap> DUMMY = LazyValue.of(() -> new SimpleCommandMap(getServer()));
    private static final Supplier<Map<String, Command>> DUMMY_VALUES = LazyValue.of(HashMap::new);

    private final Map<Class<?>, ReflectClass.Field> fields = new HashMap<>();

    BukkitCommandAPI() {
        super(ChatColor.RED.toString());
        registerDelegateArgument(Player.class, (sender, arg) -> {
            for (Player p : Bukkit.getOnlinePlayers())
                if (p.getName().equals(arg)) return p;
            throw new IllegalArgumentException("No player by name '" + arg + "' found!");
        });
        registerDelegateArgument(World.class, (sender, arg) -> {
            World w = Bukkit.getWorld(arg);
            if (w != null) return w;
            throw new IllegalArgumentException("No world by name '" + arg + "' found!");
        });
        registerDelegateArgument(Plugin.class, (sender, arg) -> {
            Plugin p = Bukkit.getPluginManager().getPlugin(arg);
            if (p != null) return p;
            throw new IllegalArgumentException("No plugin by name '" + arg + "' found!");
        });
    }

    private CommandMap getCommandMap() {
        PluginManager pm = Bukkit.getPluginManager();
        try {
            return (CommandMap) fields.computeIfAbsent(pm.getClass(), c -> new ReflectClass(c).getField("commandMap").makeAccessible()).get(pm);
        } catch (Exception ex) {
            return DUMMY.get();
        }
    }

    private Map<String, Command> getKnownCommandsMap() {
        return getKnownCommandsMap(getCommandMap());
    }

    private Map<String, Command> getKnownCommandsMap(CommandMap map) {
        try {
            return (Map<String, Command>) fields.computeIfAbsent(map.getClass(), c -> new ReflectClass(c).getField("knownCommands").makeAccessible()).get(map);
        } catch (Exception ex) {
            return DUMMY_VALUES.get();
        }
    }

    @Override
    public void sendMessage(Object sender, String message) {
        ((CommandSender) sender).sendMessage(message);
    }

    @Override
    public List<CommandSubscription<Command>> deleteCommands(BiPredicate<? super String, ? super Command> predicate) {
        Map.Entry<String, Command> entry;
        CommandMap map = getCommandMap();
        HelpMap helpMap = getServer().getHelpMap();

        List<CommandSubscription<Command>> list = new ArrayList<>();

        for (Iterator<Map.Entry<String, Command>> it = getKnownCommandsMap(map).entrySet().iterator(); it.hasNext();) {
            if (predicate.test((entry = it.next()).getKey(), entry.getValue())) {
                String label = entry.getKey();
                Command command = entry.getValue();

                it.remove();
                command.unregister(map);

                HelpTopic topic1 = helpMap.getHelpTopic(label), topic2 = helpMap.getHelpTopic("/" + label);
                if (topic1 != null) helpMap.getHelpTopics().remove(topic1);
                if (topic2 != null) helpMap.getHelpTopics().remove(topic2);

                list.add(new CommandSubscription<Command>(this, command) {
                    @Override
                    public void unsubscribe() {
                        Map<String, Command> map = getKnownCommandsMap();
                        map.put(label, command);
                        if (topic1 != null) helpMap.addTopic(topic1);
                        if (topic2 != null) helpMap.addTopic(topic2);

                        unsubscribed = true;
                    }
                });
            }
        }
        return list;
    }

    @Override
    public List<CommandSubscription<Command>> addCommandFilter(BiPredicate<? super String, ? super Command> predicate,
                                                               Predicate<? super CommandContext> filter, boolean tabComplete) {
        List<CommandSubscription<Command>> list = new ArrayList<>();

        getKnownCommandsMap().entrySet().stream().filter(entry -> predicate.test(entry.getKey(), entry.getValue())).forEach(entry -> {
            FilterCommand cmd;
            if (entry.getValue() instanceof FilterCommand)
                cmd = (FilterCommand) entry.getValue();
            else {
                cmd = new FilterCommand(entry.getValue());
                entry.setValue(cmd);
            }
            cmd.addFilter(filter, tabComplete);
        });
        return list;
    }

    @Override
    public CommandSubscription<Command> registerCommand(String plugin, Command command) {
        getCommandMap().register(plugin, command);
        HelpMap helpMap = Bukkit.getHelpMap();
        helpMap.addTopic(new GenericCommandHelpTopic(command));
        for (String alias : command.getAliases()) {
            helpMap.addTopic(new CommandAliasHelpTopic(alias, command.getLabel(), helpMap));
        }
        return new CommandSubscription<>(this, command);
    }

    @Override
    public CommandSubscription<Command> registerCommand(String plugin, CommandInfo command) {
        return registerCommand(plugin, new Command(command.name(), command.desc(),
                command.usage(), Arrays.asList(command.aliases())) {
            String permission = command.permission(), permMessage = command.permissionMessage();
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                if (permission != null && !sender.hasPermission(permission)) {
                    sender.sendMessage(permMessage != null ? permMessage : ChatColor.RED + "You don't have permission for that!");
                    return true;
                }
                command.execute(new CommandContext<>(BukkitCommandAPI.this, sender, this, label, args));
                return true;
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                TabCompleteContext<CommandSender, Command> ctx = new TabCompleteContext<>(BukkitCommandAPI.this, sender, this, alias, args);
                command.tabComplete(ctx);
                return ctx.getCompletions();
            }
        });
    }

    @Override
    public Command getCommand(String name) {
        return getKnownCommandsMap().get(name);
    }

    private class FilterCommand extends Command {

        private final Command parent;
        private final List<Predicate<? super CommandContext>> filters = new ArrayList<>(),
                                                              tabFilters = new ArrayList<>();

        private FilterCommand(Command parent) {
            super(parent.getName());
            this.parent = parent;
        }

        @Override
        public boolean setName(String name) {
            return parent.setName(name);
        }

        @Override
        public boolean setLabel(String name) {
            return parent.setLabel(name);
        }

        @Override
        public Command setDescription(String description) {
            parent.setDescription(description);
            return this;
        }

        @Override
        public Command setUsage(String usage) {
            parent.setUsage(usage);
            return this;
        }

        @Override
        public Command setAliases(List<String> aliases) {
            parent.setAliases(aliases);
            return this;
        }

        @Override
        public void setPermission(String permission) {
            parent.setPermission(permission);
        }

        @Override
        public Command setPermissionMessage(String permissionMessage) {
            parent.setPermissionMessage(permissionMessage);
            return this;
        }

        @Override
        public String getName() {
            return parent.getName();
        }

        @Override
        public String getLabel() {
            return parent.getLabel();
        }

        @Override
        public String getDescription() {
            return parent.getDescription();
        }

        @Override
        public String getUsage() {
            return parent.getUsage();
        }

        @Override
        public List<String> getAliases() {
            return parent.getAliases();
        }

        @Override
        public String getPermission() {
            return parent.getPermission();
        }

        @Override
        public String getPermissionMessage() {
            return parent.getPermissionMessage();
        }

        @Override
        public boolean testPermission(CommandSender target) {
            return parent.testPermission(target);
        }

        @Override
        public boolean testPermissionSilent(CommandSender target) {
            return parent.testPermissionSilent(target);
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            CommandContext<CommandSender, Command> ctx = new CommandContext<>(BukkitCommandAPI.this, sender, this, label, args);
            for (Predicate<? super CommandContext> filter : filters)
                if (!filter.test(ctx)) return true;
            return parent.execute(sender, label, args);
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String label, String[] args) throws IllegalArgumentException {
            TabCompleteContext<CommandSender, Command> ctx = new TabCompleteContext<>(BukkitCommandAPI.this, sender, this, label, args);
            for (Predicate<? super CommandContext> filter : tabFilters)
                if (!filter.test(ctx)) return ctx.getCompletions();
            return parent.tabComplete(sender, label, args);
        }

        @Override
        public boolean register(CommandMap commandMap) {
            return parent.register(commandMap) && super.register(commandMap);
        }

        @Override
        public boolean unregister(CommandMap commandMap) {
            return parent.unregister(commandMap) && super.unregister(commandMap);
        }

        @Override
        public boolean isRegistered() {
            return parent.isRegistered();
        }

        @Override
        public String toString() {
            return parent.toString();
        }

        CommandSubscription<Command> addFilter(Predicate<? super CommandContext> filter, boolean tabComplete) {
            filters.add(0, filter);
            if (tabComplete) tabFilters.add(0, filter);
            return new CommandSubscription<Command>(BukkitCommandAPI.this, parent) {
                @Override
                public void unsubscribe() {
                    filters.remove(filter);
                    if (tabComplete) tabFilters.remove(filter);
                    unsubscribed = true;
                }
            };
        }
    }


    private static class CommandAliasHelpTopic extends HelpTopic {

        private final String aliasFor;
        private final HelpMap helpMap;

        public CommandAliasHelpTopic(String alias, String aliasFor, HelpMap helpMap) {
            this.aliasFor = ("/" + aliasFor);
            this.helpMap = helpMap;
            this.name = ("/" + alias);
            Validate.isTrue(!this.name.equals(this.aliasFor), "Command " + this.name + " cannot be alias for itself");
            this.shortText = (ChatColor.YELLOW + "Alias for " + ChatColor.WHITE + this.aliasFor);
        }

        public String getFullText(CommandSender forWho) {
            StringBuilder sb = new StringBuilder(this.shortText);
            HelpTopic aliasForTopic = this.helpMap.getHelpTopic(this.aliasFor);
            if (aliasForTopic != null) {
                sb.append("\n").append(aliasForTopic.getFullText(forWho));
            }
            return sb.toString();
        }

        public boolean canSee(CommandSender commandSender) {
            if (this.amendedPermission == null) {
                HelpTopic aliasForTopic = this.helpMap.getHelpTopic(this.aliasFor);
                return aliasForTopic != null && aliasForTopic.canSee(commandSender);
            }
            return commandSender.hasPermission(this.amendedPermission);
        }
    }
}
