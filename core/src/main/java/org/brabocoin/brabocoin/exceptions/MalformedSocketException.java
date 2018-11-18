package org.brabocoin.brabocoin.exceptions;

/**
 * An exception that is thrown when the string representation for a socket is malformed.
 *
 * The default socket format for IPv4 is {hostname or ip}:{port}
 */
public class MalformedSocketException extends Exception {
    public MalformedSocketException(String message) {
        super(message);
    }
}
