package me.megamichiel.animationlib.command.arg;

import me.megamichiel.animationlib.command.CommandAPI;

/**
 * This class can be used to register a class that doesn't implement CustomArgument as CommandAPI valid argument.
 * For this class to take effect, the method {@link CommandAPI#registerDelegateArgument(Class, DelegateArgument)} must be called.
 * <p>
 * If you wish to specify this argument's {@link Priority} (Default {@link Priority#NORMAL}), create a static method getPriority() returning the desired Priority.
 * <p>
 * The Player class, for example, is implemented using:
 * <p>
 * public Player parse(CommandSender sender, String value) throws Exception {<br/>
 * &nbsp; Player player = Bukkit.getPlayerExact(value);<br/>
 * &nbsp; if (player != null) return player;<br/>
 * &nbsp; throw new CommandException("No player found by name \"" + value + "\"!");<br/>
 * }<br/>
 * It's priority is {@link Priority#NORMAL}
 * 
 * @author Michiel
 *
 * @param <S> The sender of the command
 * @param <T> the type this class will be able to parse
 */
public interface DelegateArgument<S, T> {
    
    /**
     * The method to turn an argument into the target value.<br/>
     * If this method throws an exception, the sender will be sent the exception's message.
     * 
     * @param sender the sender of the command
     * @param value the value to parse into <i><b>T</b></i>
     * @return The created value. The creator may choose to return null, although this is not recommended.
     * @throws IllegalArgumentException If no corresponding value was found.
     */
    T parse(S sender, String value) throws IllegalArgumentException;

    /**
     * Returns the priority of this argument.
     */
    default Priority getPriority() {
        return Priority.NORMAL;
    }
}
