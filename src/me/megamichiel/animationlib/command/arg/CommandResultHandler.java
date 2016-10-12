package me.megamichiel.animationlib.command.arg;

/**
 * A class that can handle custom command results.
 *
 * @param <S> the type of the command sender
 * @param <T> the type the handler will handle
 */
public interface CommandResultHandler<S, T> {

    void onCommandResult(S sender, T result);
}
