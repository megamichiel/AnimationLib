package me.megamichiel.animationlib.command;

import me.megamichiel.animationlib.command.annotation.CommandHandler;
import me.megamichiel.animationlib.command.arg.CommandResultHandler;
import me.megamichiel.animationlib.command.arg.CustomArgument;
import me.megamichiel.animationlib.command.arg.DelegateArgument;
import me.megamichiel.animationlib.command.arg.EntitySelector;
import me.megamichiel.animationlib.command.exec.CommandAdapter;
import me.megamichiel.animationlib.command.exec.CommandContext;
import me.megamichiel.animationlib.util.Subscription;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public interface CommandAPI<S, C> {
    
    /**
     * Registers all methods of this class as command which meet the following requirements:
     * <ul>
     *  <li>The method must be annotated with {@link CommandHandler}</li>
     *  <li>The method must start with 2 arguments: {@link S} and {@link String} (the command label)</li>
     *  <li>
     *      All of the method's parameters' types must be recognized by the API.
     *      By default, these types will be parsable:
     *      <ul>
     *          <li>String (Of course)</li>
     *          <li>Primitive types (byte, short, int etc.) and their wrappers (Byte, Short, Integer etc.)</li>
     *          <li>Enums (Using valueOf(String) with the argument upper-cased and dashes '-' replaced by underscores '_')</li>
     *          <li>
     *              Arrays (1 dimension only) are also supported, as long as the component type is recognized<br/>
     *              Arrays are created using multiple arguments.<br/>
     *              For example, if a Command's arguments consist of a String array only, all arguments are put into that array.
     *          </li>
     *          <li>If you wish to add a custom class, it should implement {@link CustomArgument} and follow it's rules.</li>
     *          <li>If you wish to add a class that doesn't implement {@link CustomArgument}, but do want to be able to parse it,
     *              see {@link DelegateArgument}</li>
     *          <li>The {@link EntitySelector} class is a built-in {@link CustomArgument} which (partially) mimics minecraft's target selector</li>
     *      </ul>
     *  </li>
     *  <li>The method does <i>not</i> have to return a boolean (can be void as well). If it does and the method returns false, the player will get sent the usage message. Otherwise nothing will happen</li>
     * </ul>
     * 
     * @param plugin the plugin linked to this command adapter
     * @param adapter the command adapter
     * 
     * @throws NullPointerException if plugin or adapter is null
     */
    List<CommandSubscription<C>> registerCommands(String plugin, CommandAdapter adapter);

    /**
     * Removes known commands from the existing registry. Stuff like /plugins can be removed.
     *
     * @param names the command names
     */
    List<CommandSubscription<C>> deleteCommands(String... names);

    /**
     * Removes known commands from the existing registry, as specified by the predicate
     *
     * @param predicate the acceptor for the commands, where the key is the known label and the value the corresponding command
     */
    List<CommandSubscription<C>> deleteCommands(BiPredicate<? super String, ? super C> predicate);

    /**
     * Registers a filter for a command
     *
     * @param command      the command to add a filter to
     * @param predicate    if the predicate returns true, the command should be executed.
     */
    CommandSubscription<C> addCommandFilter(String command, Predicate<? super CommandContext> predicate, boolean tabComplete);

    /**
     * Registers a filter for a command
     *
     * @param predicate the command filter to specify which commands to filter
     * @param filter if the predicate returns true, the command should be executed.
     * @param tabComplete
     */
    List<CommandSubscription<C>> addCommandFilter(BiPredicate<? super String, ? super C> predicate,
                                               Predicate<? super CommandContext> filter,
                                               boolean tabComplete);

    /**
     * Registers a type as a valid argument for types which don't implement
     * {@link CustomArgument}
     * 
     * @param type the type to register
     * @param delegate the instance that parses a String into the type
     * 
     * @throws NullPointerException if type or delegate is null
     * @throws IllegalArgumentException if type implements CustomArgument
     * @throws IllegalStateException if there already is a DelegateArgument registered to this type
     */
    <T> Subscription registerDelegateArgument(Class<? super T> type, DelegateArgument<S, ? extends T> delegate);

    /**
     * Registers a new CommandResultHandler
     *
     * @param handler the CommandResultHandler to register
     */
    <T> Subscription registerCommandResultHandler(Class<? extends T> clazz, CommandResultHandler<S, ? super T> handler);

    /**
     * Registers a command directly into the server's command map
     *
     * @param plugin the owning plugin
     * @param command the command to register
     */
    CommandSubscription<C> registerCommand(String plugin, C command);

    /**
     * Registers a command directly into the server's command map
     *
     * @param plugin the owning plugin
     * @param command the command to register
     */
    CommandSubscription<C> registerCommand(String plugin, CommandInfo command);

    /**
     * Retrieves a command by name
     * 
     * @param name the name of the command
     * @return a command by this name, may be null
     */
    C getCommand(String name);
}
