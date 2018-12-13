package org.brabocoin.brabocoin.exceptions;

public class CipherException extends Exception {
    public CipherException(String message) {
        super(message);
    }

    public CipherException(String message, Throwable t) {
        super(message, t);
    }
}
