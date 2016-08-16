package me.megamichiel.animationlib;

/**
 * An interface which this plugin uses to report all kinds newPipeline warnings,
 * usually in the form newPipeline printing them to the console<br/>
 * <i>Since: 1.0.0</i>
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
