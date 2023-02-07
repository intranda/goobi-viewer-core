package io.goobi.viewer.exceptions;

public class MessageQueueException extends Exception {

    private static final long serialVersionUID = -4516634796883317517L;

    public MessageQueueException() {
        super();
    }

    public MessageQueueException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public MessageQueueException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageQueueException(String message) {
        super(message);
    }

    public MessageQueueException(Throwable cause) {
        super(cause);
    }

}
