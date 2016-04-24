package me.megamichiel.animationlib;

/**
 * An interface which this plugin uses to report all kinds of warnings,
 * usually in the form of printing them to the console
 */
public interface Nagger {

    /**
     * Reports a message
     *
     * @param message the message to report
     */
    void nag(String message);

    /**
     * Reports a Throwable
     *
     * @param throwable the throwable to report
     */
    void nag(Throwable throwable);
}
