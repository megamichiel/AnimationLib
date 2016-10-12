package me.megamichiel.animationlib.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that differs per context.<br/>
 * Use on:
 * <ul>
 *  <li>
 *   A {@link CommandHandler}'s sender to specify then name of the sender, for when the sender is not of that type.<br/>
 *   For example, &#64;<i>Alias("a player") Player sender</i> will make the console get sent "You must be <i>a player</i> for that!"<br/>
 *   By default, the alias of Player is "a player" and that of ConsoleCommandSender is "the console"
 *  </li>
 *  <li>A {@link CommandHandler}'s parameter to specify the name of the parameter used in the command's usage.</li>
 * </ul>
 * 
 * @author Michiel
 *
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.METHOD })
public @interface Alias {
	
	/**
	 * The value of this alias
	 * 
	 * @return the alias name
	 */
	String value();
}
