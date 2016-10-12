package me.megamichiel.animationlib.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be added to a {@link CommandHandler} parameter to specify that the argument is not required.<br/>
 * The {@link #value()} member can be used to specify the default value.
 * 
 * @author Michiel
 *
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Optional {
	
	/**
	 * Set this to specify the default value of an argument.
	 * 
	 * @return the default value
	 */
	String value() default "@null";
	/**
	 * An array of classes which the sender must be for the argument to be optional.<br/>
	 * For example, <i>&#64;Optional(asSender = Player.class)</i> specifies that this argument is only optional if the sender is a Player<br/>
	 * This could be used for things like a teleport command, where the console needs to specify the player, but a player could teleport themselves.
	 * 
	 * @return An array of classes
	 */
	Class<?>[] asSender() default {};
}
