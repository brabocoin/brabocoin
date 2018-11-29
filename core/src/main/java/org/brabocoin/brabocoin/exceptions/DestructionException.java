package org.brabocoin.brabocoin.exceptions;

public class DestructionException extends Exception {
    public DestructionException(String message) {
        super(message);
    }

    public DestructionException(String message, Throwable t) {
        super(message, t);
    }
}
