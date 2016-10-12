package me.megamichiel.animationlib.command.arg;

/**
 * A class implementing this interface can be used as a CommandAPI registered command's parameter.
 * For a class to properly function as method function, it needs to meet these two requirements:
 * <ul>
 *  <li>The class must implement CustomArgument, of course.</li>
 *  <li>The class must have a (CommandSender, String) constructor (access level doesn't matter)</li>
 * </ul>
 * If you wish to specify this argument's {@link Priority} (Default {@link Priority#NORMAL}), create a static method getPriority() returning the desired Priority.
 * 
 * @author Michiel
 *
 */
public interface CustomArgument {}
