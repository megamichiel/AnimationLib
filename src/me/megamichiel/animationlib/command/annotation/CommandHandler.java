package me.megamichiel.animationlib.command.annotation;

import me.megamichiel.animationlib.command.CommandAPI;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation to give a method for CommandAPI#registerCommands to know it is a command.
 * <p>
 * By default, the command's name will be set to the method's name.
 * This can be overridden by adding the {@link Alias} annotation to the command, with the value set to the command's name.
 * <p>
 * If the {@link #usage()} value is not specified, {@link CommandAPI} will create a usage message based on the method's parameters<br/>
 * The {@link CommandArguments} annotation can be added to the method to specify the method's arguments names.<br/>
 * An argument can also be given the {@link Alias} annotation to specify it's name.<br/>
 * If an argument's name is not given by either {@link CommandArguments} or {@link Alias},
 * the lower-cased version of the type's simple name is used. Player would become player, World world, etc.
 * 
 * @author Michiel
 * 
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandHandler {

    /**
     * The name + aliases of this command. By default this is <i>&lt;method&gt</i>, which uses the method's name
     *
     * @return the command's name
     */
    String[] value() default "<method>";
    
    /**
     * The description of this command. This will be visible in most /help menus
     * 
     * @return the description of the command.
     */
    String desc() default "A CommandAPI created command";
    /**
     * The usage of this command. If this is not specified, {@link CommandAPI} will create a usage message based on the command's arguments.
     * 
     * @return The usage message
     */
    String usage() default "";
    /**
     * The permission required to use this command.
     * 
     * @return The required permission
     */
    String perm() default "";
    /**
     * The message sent to the sender if they don't have permission {@link #perm()}
     * @return The message to be sent.
     */
    String permMessage() default "";
}
