package org.brabocoin.brabocoin.exceptions;

public class StateInitializationException extends RuntimeException {
    public StateInitializationException(String message) {
        super(message);
    }

    public StateInitializationException(String message, Throwable t) {
        super(message, t);
    }
}
