package me.megamichiel.animationlib.command.arg;

/**
 * The priority of an argument.
 * The lower the priority, the smaller the chance that a parse call on an argument will fail.
 * Vice versa, the higher the priority, the bigger the change that a parse call will fail.
 * 
 * @author Michiel
 *
 */
public enum Priority {
	
	/**
	 * The lowest priority. An argument with this priority is very unlikely to fail.
	 * An example argument type that uses this is String
	 */
	LOW,
	/**
	 * A 'normal' priority. Although arguments with this type may not always succeed on parsing,
	 * they could still fail.
	 */
	NORMAL,
	/**
	 * The highest priority. The chance is relatively big that parsing an argument with this priority will fail.
	 * Example arguments with this priority are primitive types (int, double, float, long etc.) as they only allow a specific list of characters.
	 */
	HIGH;
}
