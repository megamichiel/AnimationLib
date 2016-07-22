package me.megamichiel.animationlib.config.serialize;

public class ConfigurationSerializationException extends RuntimeException {

    private final String path;
    private final Type type;
    private final String reason;

    public ConfigurationSerializationException(String path,
                                               Type type,
                                               Throwable cause,
                                               String reason) {
        super(type.message + " at '" + path + "': " + reason, cause);
        this.path = path;
        this.type = type;
        this.reason = reason;
    }

    public String getPath() {
        return path;
    }

    public Type getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public enum Type {
        NEW_INSTANCE("Unable to create instance"),
        NO_CONSTRUCTOR_ACCESS("No constructor access"),
        NO_FIELD_ACCESS("No field access"),
        INVALID_TYPE("Invalid type");

        private final String message;

        Type(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
