package org.brabocoin.brabocoin.exceptions;

/**
 * Represents an all-purpose database exception.
 */
public class DatabaseException extends Exception {
    public DatabaseException(String message) {
        super(message);
    }
}