package org.brabocoin.brabocoin.exceptions;

public class IllegalConfigMappingException extends Exception {
    public IllegalConfigMappingException(String message) {
        super(message);
    }

    public IllegalConfigMappingException(String message, Throwable t) {
        super(message, t);
    }
}
