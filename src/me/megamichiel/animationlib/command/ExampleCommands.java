package me.megamichiel.animationlib.command;


import com.google.common.base.Joiner;
import me.megamichiel.animationlib.bukkit.AnimLibPlugin;
import me.megamichiel.animationlib.command.annotation.Alias;
import me.megamichiel.animationlib.command.annotation.CommandArguments;
import me.megamichiel.animationlib.command.annotation.CommandHandler;
import me.megamichiel.animationlib.command.annotation.Optional;
import me.megamichiel.animationlib.command.arg.EntitySelector;
import me.megamichiel.animationlib.command.exec.CommandAdapter;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ExampleCommands implements CommandAdapter {

    public ExampleCommands(String plugin) {
        AnimLibPlugin.getInstance().getCommandAPI().registerCommands(plugin, this);
    }
    
    @SuppressWarnings("deprecation")
    @CommandHandler(value = "message", usage = "/<command> <player> <message...>")
    boolean normalCommand(CommandSender sender, String label, @Optional String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player for that!");
            return true;
        }
        if (args.length < 2)
            return false;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "No player by name \"" + args[0] + "\" found!");
            return true;
        }
        String suffix = ChatColor.RED + " > " + ChatColor.GREEN + String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        sender.sendMessage(ChatColor.GOLD + "To " + target.getName() + suffix);
        target.sendMessage(ChatColor.GOLD + "From " + sender.getName() + suffix);
        return true;
    }
    
    @CommandHandler("messagenew")
    String newCommand(Player sender, String label, Player target, @Alias("message") String[] message) {
        String suffix = ChatColor.RED + " > " + ChatColor.GREEN + String.join(" ", message);
        target.sendMessage(ChatColor.GOLD + "From " + sender.getName() + suffix);
		return ChatColor.GOLD + "To " + target.getName() + suffix;
    }
    
    @CommandHandler
    String feed(Player player, String label) {
        player.setFoodLevel(20);
        return ChatColor.GREEN + "Your appetite has been sated.";
    }
    
    @CommandHandler
    @CommandArguments({"player", "type", "amount", "data"})
    String give(CommandSender sender, String label,
              @Optional(asSender = Player.class) Player target, Material type,
              @Optional("1") int amount, @Optional short data) {
        ItemStack item = new ItemStack(type, amount, data);
        if (target == null) target = (Player) sender;
        target.getInventory().addItem(item);
        return ChatColor.GREEN + "Given " + amount + "x" + type.name() + ":" + data + " to " + target.getName();
    }
    
    @CommandHandler("entities")
    String tellMehEntities(CommandSender sender, String label, @Alias("selector") EntitySelector selector) {
        Iterator<Entity> it = selector.getEntities().iterator();
        if (!it.hasNext()) {
            return ChatColor.RED + "No entities found :(";
        }
        StringBuilder sb = new StringBuilder(it.next().getName());
        while (it.hasNext()) {
            sb.append(", ").append(it.next());
        }
        return ChatColor.GREEN + "Entities: " + sb.toString();
    }
    
    @CommandHandler
    @CommandArguments({"target", "world", "x", "y", "z", "yaw", "pitch"})
    String teleport(CommandSender sender, String label,
            @Optional(asSender = Player.class) EntitySelector target,
            @Optional World world, double x, double y, double z,
            @Optional float yaw, @Optional float pitch) {
        List<Entity> entities;
        if (target != null) {
            entities = target.getEntities();
        } else {
            entities = Collections.singletonList((Player) sender); // Only optional if sender is a player, so casting is fine ;3
        }
        if (entities.isEmpty()) {
            return ChatColor.RED + "No entities found!";
        }
		if (world == null) {
            world = sender instanceof Player ? ((Player) sender).getWorld() : Bukkit.getWorlds().get(0);
        }
		Location loc = new Location(world, x, y, z, yaw, pitch);
        for (Entity entity : entities) {
            entity.teleport(loc);
        }
		return null;
    }
}
