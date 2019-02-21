package org.brabocoin.brabocoin.exceptions;

public class InsufficientInputException extends Exception {
    public InsufficientInputException(String message) {
        super(message);
    }

    public InsufficientInputException(String message, Throwable t) {
        super(message, t);
    }
}
