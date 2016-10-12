package me.megamichiel.animationlib.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to specify the argument names of a {@link CommandHandler} to be used in the usage message.
 * This will only be applied if the length of {@link #value()} is the same as that of the command argument's length.
 * <p>
 * For example, a command: teleport(CommandSender sender, String label, int x, int y, int z)
 * would have a CommandArguments annotation: &#64;CommandArguments({"x", "y", "z"})
 * 
 * @author Michiel
 *
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandArguments {
	
	/**
	 * The argument names of this command
	 * 
	 * @return An array of names
	 */
	String[] value();
}
