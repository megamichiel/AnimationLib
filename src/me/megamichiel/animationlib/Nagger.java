package me.megamichiel.animationlib;

/**
 * An interface which this plugin uses to report all kinds of warnings,
 * usually in the form of printing them to the console<br/>
 * <i>Since: 1.0.0</i>
 */
public interface Nagger {

    Nagger ILLEGAL_ARGUMENT = new Nagger() {
        @Override
        public void nag(String message) {
            throw new IllegalArgumentException(message);
        }

        @Override
        public void nag(Throwable throwable) {
            throw new IllegalArgumentException(throwable);
        }
    };

    /**
     * Reports a message
     *
     * @param message the message to report
     */
    void nag(String message);

    /**
     * Reports a message using the given format
     *
     * @param format The format of the text
     * @param args The arguments to use in the text
     */
    default void nag(String format, Object... args) {
        nag(String.format(format, args));
    }

    /**
     * Reports a Throwable
     *
     * @param throwable the throwable to report
     */
    void nag(Throwable throwable);
}
