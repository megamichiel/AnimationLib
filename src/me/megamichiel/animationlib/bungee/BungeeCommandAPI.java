package me.megamichiel.animationlib.bungee;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import me.megamichiel.animationlib.command.BaseCommandAPI;
import me.megamichiel.animationlib.command.CommandInfo;
import me.megamichiel.animationlib.command.CommandSubscription;
import me.megamichiel.animationlib.command.exec.CommandContext;
import me.megamichiel.animationlib.command.exec.TabCompleteContext;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class BungeeCommandAPI extends BaseCommandAPI<Plugin, CommandSender, Command> {

    public static BungeeCommandAPI getInstance() {
        return (BungeeCommandAPI) BaseCommandAPI.<Plugin, CommandSender, Command>getInstance();
    }

    private final Map<Class<?>, Field> commandMap = new HashMap<>(),
                                       commandsByPlugin = new HashMap<>();

    {
        registerDelegateArgument(ProxiedPlayer.class, (sender, arg) -> {
            ProxiedPlayer player = BungeeCord.getInstance().getPlayer(arg);
            if (player != null) return player;
            throw new IllegalArgumentException("No server by name '" + arg + "' found!");
        });
        registerDelegateArgument(ServerInfo.class, (sender, arg) -> {
            ServerInfo info = BungeeCord.getInstance().getServerInfo(arg);
            if (info != null) return info;
            throw new IllegalArgumentException("No server by name '" + arg + "' found!");
        });
        registerDelegateArgument(Plugin.class, (sender, arg) -> {
            Plugin p = BungeeCord.getInstance().getPluginManager().getPlugin(arg);
            if (p != null) return p;
            throw new IllegalArgumentException("No plugin by name '" + arg + "' found!");
        });
    }

    private Map<String, Command> getCommandMap() {
        PluginManager pm = BungeeCord.getInstance().getPluginManager();
        Field field = commandMap.computeIfAbsent(pm.getClass(), c -> {
            try {
                Field f = c.getDeclaredField("commandMap");
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                return null;
            }
        });
        try {
            return (Map<String, Command>) field.get(pm);
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }

    private Multimap<Plugin, Command> getCommandsByPlugin() {
        PluginManager pm = BungeeCord.getInstance().getPluginManager();
        Field field = commandsByPlugin.computeIfAbsent(pm.getClass(), c -> {
            try {
                Field f = c.getDeclaredField("commandsByPlugin");
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                return null;
            }
        });
        try {
            return (Multimap<Plugin, Command>) field.get(pm);
        } catch (Exception ex) {
            return HashMultimap.create();
        }
    }

    @Override
    public void sendMessage(Object sender, String message) {
        ((ProxiedPlayer) sender).sendMessage(TextComponent.fromLegacyText(message));
    }

    @Override
    public String red() {
        return ChatColor.RED.toString();
    }

    @Override
    public List<CommandSubscription<Command>> deleteCommands(BiPredicate<? super String, ? super Command> predicate) {
        Map.Entry<String, Command> entry;
        List<CommandSubscription<Command>> list = new ArrayList<>();
        for (Iterator<Map.Entry<String, Command>> it = getCommandMap().entrySet().iterator(); it.hasNext(); ) {
            if (predicate.test((entry = it.next()).getKey(), entry.getValue())) {
                String label = entry.getKey();
                Command command = entry.getValue();
                Multimap<Plugin, Command> byPlugin = getCommandsByPlugin();
                Plugin plugin = getPlugin(byPlugin, command);

                list.add(new CommandSubscription<Command>(this, command) {
                    @Override
                    public void unsubscribe() {
                        getCommandMap().put(label, command);

                        Collection<Command> commands = getCommandsByPlugin().get(plugin);
                        if (!commands.contains(command))
                            commands.add(command);

                        unsubscribed = true;
                    }
                });

                it.remove();
                byPlugin.values().remove(command);
            }
        }
        return list;
    }

    private Plugin getPlugin(Multimap<Plugin, Command> byPlugin, Command command) {
        for (Map.Entry<Plugin, Command> entry : byPlugin.entries())
            if (entry.getValue() == command) return entry.getKey();
        return null;
    }

    @Override
    public List<CommandSubscription<Command>> addCommandFilter(BiPredicate<? super String, ? super Command> predicate, Predicate<? super CommandContext> filter, boolean tabComplete) {
        List<CommandSubscription<Command>> list = new ArrayList<>();

        Command value;
        for (Map.Entry<String, Command> entry : getCommandMap().entrySet())
            if (predicate.test(entry.getKey(), entry.getValue())) {
                FilterCommand cmd;
                if ((value = entry.getValue()) instanceof FilterCommand)
                    cmd = (FilterCommand) value;
                else entry.setValue(cmd = tabComplete ? new FilterTabCommand(this, value) : new FilterCommand(this, value));
                cmd.addFilter(filter, tabComplete);
            }
        return list;
    }

    @Override
    public CommandSubscription<Command> registerCommand(Plugin plugin, Command command) {
        BungeeCord.getInstance().getPluginManager().registerCommand(plugin, command);
        return new CommandSubscription<>(this, command);
    }

    @Override
    public CommandSubscription<Command> registerCommand(Plugin plugin, CommandInfo command) {
        return registerCommand(plugin, new TabCommand(command.name(), null, command.aliases()) {
            private String permission = command.permission(), permissionMessage = command.permissionMessage();
            @Override
            public void execute(CommandSender sender, String[] args) {
                if (permission != null && !sender.hasPermission(permission)) {
                    sendMessage(sender, permissionMessage == null ? red() + "You don't have permssion for that!" : permissionMessage);
                    return;
                }
                command.execute(new CommandContext<>(BungeeCommandAPI.this, sender, this, command.name(), args));
            }

            @Override
            public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
                if (permission != null && !sender.hasPermission(permissionMessage))
                    return Collections.emptyList();
                TabCompleteContext<CommandSender, Command> ctx = new TabCompleteContext<>(BungeeCommandAPI.this, sender, this, command.name(), args);
                command.tabComplete(ctx);
                return ctx.getCompletions();
            }
        });
    }

    @Override
    public Command getCommand(String name) {
        return getCommandMap().get(name);
    }

    private static abstract class TabCommand extends Command implements TabExecutor {

        TabCommand(String name, String permission, String... aliases) {
            super(name, permission, aliases);
        }
    }

    private static class FilterCommand extends Command {

        final BungeeCommandAPI api;
        final Command parent;
        private final List<Predicate<? super CommandContext<CommandSender, Command>>> filters = new ArrayList<>();

        FilterCommand(BungeeCommandAPI api, Command parent) {
            super(parent.getName(), parent.getPermission(), parent.getAliases());
            this.api = api;
            this.parent = parent;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            CommandContext<CommandSender, Command> ctx = new CommandContext<>(api, sender, this, getName(), args);
            for (Predicate<? super CommandContext<CommandSender, Command>> filter : filters)
                if (!filter.test(ctx)) return;
            parent.execute(sender, args);
        }

        void addFilter(Predicate<? super CommandContext<CommandSender, Command>> filter,
                       boolean tabComplete) {
            filters.add(filter);
        }
    }

    private static class FilterTabCommand extends FilterCommand implements TabExecutor {

        private final List<Predicate<? super CommandContext<CommandSender, Command>>> tabFilters = new ArrayList<>();

        FilterTabCommand(BungeeCommandAPI api, Command parent) {
            super(api, parent);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            TabCompleteContext<CommandSender, Command> ctx = new TabCompleteContext<>(api, sender, this, getName(), args);
            for (Predicate<? super TabCompleteContext<CommandSender, Command>> filter : tabFilters)
                if (!filter.test(ctx)) return ctx.getCompletions();
            if (parent instanceof TabExecutor)
                return ((TabExecutor) parent).onTabComplete(sender, args);
            return Collections.emptyList();
        }

        @Override
        void addFilter(Predicate<? super CommandContext<CommandSender, Command>> filter, boolean tabComplete) {
            super.addFilter(filter, tabComplete);
            if (tabComplete) tabFilters.add(filter);
        }
    }
}
