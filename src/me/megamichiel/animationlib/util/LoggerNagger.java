package me.megamichiel.animationlib.util;

import me.megamichiel.animationlib.Nagger;

import java.util.logging.Logger;

public interface LoggerNagger extends Nagger {

    static LoggerNagger of(Logger logger) {
        return () -> logger;
    }

    Logger getLogger();

    @Override
    default void nag(String message) {
        getLogger().warning(message);
    }

    @Override
    default void nag(Throwable throwable) {
        getLogger().warning(throwable.toString());
    }
}
