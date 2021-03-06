package org.brabocoin.brabocoin.node;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.exceptions.MalformedSocketException;
import org.brabocoin.brabocoin.proto.services.NodeGrpc;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A representation of an peer in the network.
 */
public class Peer implements NetworkMessageListener {

    private static final Logger LOGGER = Logger.getLogger(Peer.class.getName());
    /**
     * The socket for this peer.
     */
    private InetSocketAddress socket;

    private ManagedChannel channel;
    private NodeGrpc.NodeBlockingStub blockingStub;
    private NodeGrpc.NodeStub asyncStub;
    private PeerMessageInterceptor interceptor;

    private Queue<NetworkMessage> outgoingMessageQueue =
        new CircularFifoQueue<>(Constants.MAX_MESSAGE_HISTORY_PER_PEER);

    private Queue<NetworkMessage> incomingMessageQueue =
        new CircularFifoQueue<>(Constants.MAX_MESSAGE_HISTORY_PER_PEER);

    private final List<NetworkMessageListener> networkMessageListeners = new ArrayList<>();

    /**
     * Creates a peer from an address and port.
     *
     * @param address
     *     The address of the peer.
     * @param port
     *     The port of the peer
     */
    public Peer(InetAddress address, int port) throws MalformedSocketException {
        this(new InetSocketAddress(address, port));
    }

    /**
     * Creates a peer from a given socket address.
     *
     * @param socketAddress
     *     The socket of the peer.
     */
    public Peer(InetSocketAddress socketAddress) throws MalformedSocketException {
        this.socket = socketAddress;
        setupStubs();
    }


    /**
     * Creates a peer from a string representation of a socket.
     *
     * @param socket
     *     The string representation of a socket {hostname}:{port}
     * @throws MalformedSocketException
     *     Thrown when the socket is malformed.
     */
    public Peer(String socket) throws MalformedSocketException {
        this.socket = getSocketFromString(socket);
        setupStubs();
    }


    /**
     * Check whether this peer is a connection to the local machine.
     *
     * @return Whether this peer is the local machine.
     */
    public boolean isLocal() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = (InetAddress)addresses.nextElement();
                    if (address.equals(this.getAddress())) {
                        return true;
                    }
                }
            }
        }
        catch (SocketException e) {
            e.printStackTrace();
        }

        try {
            return socket.getAddress().isAnyLocalAddress() || socket.getAddress()
                .isLoopbackAddress() ||
                InetAddress.getLocalHost().equals(this.getAddress());
        }
        catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * Get the port of this peer.
     *
     * @return Port of this peer.
     */
    public int getPort() {
        return socket.getPort();
    }

    /**
     * Create a channel for the peer address and setup stubs to communicate with this peer.
     * Also add a shutdown handler for the channel.
     */
    private void setupStubs() throws MalformedSocketException {
        LOGGER.log(Level.FINE, "Setting up a new peer: {0}", toSocketString());
        this.interceptor = new PeerMessageInterceptor(this);
        this.interceptor.addNetworkMessageListener(this);
        try {
            this.channel = ManagedChannelBuilder
                .forAddress(socket.getHostString(), socket.getPort())
                .intercept(interceptor.createClientCallInterceptor())
                .usePlaintext()
                .build();
        }
        catch (IllegalArgumentException e) {
            LOGGER.log(Level.FINE, "Could not build channel for peer: {0}", e.getMessage());
            // hostname or port is invalid
            throw new MalformedSocketException("Invalid hostname or port on channel creation.");
        }
        this.blockingStub = NodeGrpc.newBlockingStub(channel);
        this.asyncStub = NodeGrpc.newStub(channel);


        Runtime.getRuntime().addShutdownHook(new Thread(channel::shutdown));
    }

    public void addNetworkMessageListener(NetworkMessageListener listener) {
        networkMessageListeners.add(listener);
    }

    public void removeNetworkMessageListeners(NetworkMessageListener listener) {
        networkMessageListeners.remove(listener);
    }

    @Override
    public void onOutgoingMessage(NetworkMessage message, boolean isUpdate) {
        if (!isUpdate && !outgoingMessageQueue.contains(message)) {
            outgoingMessageQueue.add(message);
        }
        networkMessageListeners.forEach(l -> l.onOutgoingMessage(message, isUpdate));
    }

    public synchronized Collection<NetworkMessage> getOutgoingMessageQueue() {
        return Collections.unmodifiableCollection(outgoingMessageQueue);
    }

    public synchronized Collection<NetworkMessage> getIncomingMessageQueue() {
        return Collections.unmodifiableCollection(incomingMessageQueue);
    }

    /**
     * Close the channel to this peer.
     */
    public void shutdown() {
        LOGGER.log(Level.INFO, "Stopping peer: {0}", toSocketString());
        this.interceptor.removeNetworkMessageListeners(this);
        channel.shutdown();
    }

    public void restart() throws MalformedSocketException {
        setupStubs();
    }

    /**
     * Returns whether the channel for this peer is still open and not terminated.
     *
     * @return Boolean indicating peer channel availability.
     */
    public boolean isRunning() {
        return !channel.isShutdown() && !channel.isTerminated();
    }

    /**
     * Tries to parse a InetSocketAddress from a {ip}:{port} string.
     *
     * @param socket
     *     Socket in string format
     * @return Instantiated InetSocketAddress
     * @throws MalformedSocketException
     *     When the socket string has an invalid format.
     */
    private InetSocketAddress getSocketFromString(String socket) throws MalformedSocketException {
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

    /**
     * Get the blocking stub.
     *
     * @return NodeBlockingStub for this peer.
     */
    public NodeGrpc.NodeBlockingStub getBlockingStub() {
        return blockingStub;
    }

    /**
     * Get the async stub.
     *
     * @return NodeStub for this peer.
     */
    public NodeGrpc.NodeStub getAsyncStub() {
        return asyncStub;
    }

    @Override
    public String toString() {
        return String.format(
            "%s:%d (%s)",
            socket.getHostString(),
            socket.getPort(),
            isRunning() ? "running" : "closed"
        );
    }

    /**
     * Gets the socket string for this peer.
     *
     * @return Socket string in {ip}:{port} format.
     */
    public String toSocketString() {
        return String.format("%s:%d", socket.getHostName(), socket.getPort());
    }

    @Override
    public int hashCode() {
        return socket.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Peer other = (Peer)obj;
        return socket.getAddress().equals(other.socket.getAddress()) &&
            socket.getPort() == other.socket.getPort();
    }

    /**
     * Get the address of this peer.
     *
     * @return The peer address.
     */
    public InetAddress getAddress() {
        return socket.getAddress();
    }

    /**
     * Add a message to this peer's incoming messages.
     *
     * @param networkMessage
     *     The messsage to add.
     */
    public void addIncomingMessage(NetworkMessage networkMessage) {
        incomingMessageQueue.add(networkMessage);
    }
}
