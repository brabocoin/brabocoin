package org.brabocoin.brabocoin.node;

import org.brabocoin.brabocoin.exceptions.MalformedSocketException;

import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkUtil {

    private static final Logger LOGGER = Logger.getLogger(NetworkUtil.class.getName());

    /**
     * Tries to parse a InetSocketAddress from a {ip}:{port} string.
     *
     * @param socket
     *     Socket in string format
     * @return Instantiated InetSocketAddress
     * @throws MalformedSocketException
     *     When the socket string has an invalid format.
     */
    public static InetSocketAddress getSocketFromString(
        String socket) throws MalformedSocketException {
        LOGGER.fine("Getting socket from string.");
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("String: {0}", socket));
        if (!socket.contains(":")) {
            LOGGER.log(Level.WARNING, "Socket failed to parse, invalid amount of colons.");
            throw new MalformedSocketException(
                "Socket representation does not contain a colon separator.");
        }

        String[] socketSplit = socket.split(":");
        if (socketSplit.length != 2) {
            LOGGER.log(Level.WARNING, "Socket failed to parse, invalid amount of colons.");
            throw new MalformedSocketException(
                "Socket representation does not contain a single separator.");
        }

        int socketPort;
        try {
            socketPort = Integer.parseInt(socketSplit[1]);
        }
        catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Socket failed to parse: {0}", e.getMessage());
            throw new MalformedSocketException("Socket port section is not an integer.");
        }

        InetSocketAddress socketAddress;
        try {
            socketAddress = new InetSocketAddress(socketSplit[0], socketPort);
        }
        catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Socket failed to parse: {0}", e.getMessage());
            throw new MalformedSocketException(e.getMessage());
        }


        LOGGER.fine("Socket created.");
        return socketAddress;
    }
}
