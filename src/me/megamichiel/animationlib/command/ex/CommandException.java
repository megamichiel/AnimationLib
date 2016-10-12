package me.megamichiel.animationlib.command.ex;

public class CommandException extends RuntimeException {

    private static final long serialVersionUID = 3481069473026781359L;

    public CommandException(String message) {
        super(message);
    }
}
