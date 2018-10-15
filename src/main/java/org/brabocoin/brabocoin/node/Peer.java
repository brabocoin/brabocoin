package org.brabocoin.brabocoin.node;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.brabocoin.brabocoin.exceptions.MalformedSocketException;
import org.brabocoin.brabocoin.proto.services.NodeGrpc;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * A representation of an peer in the network.
 */
public class Peer {
    /**
     * The socket for this peer.
     */
    private InetSocketAddress socket;

    private ManagedChannel channel;
    public NodeGrpc.NodeBlockingStub blockingStub;
    public NodeGrpc.NodeStub asyncStub;

    /**
     * Creates a peer from an address and port.
     *
     * @param address The address of the peer.
     * @param port    The port of the peer
     */
    public Peer(InetAddress address, int port) {
        this(new InetSocketAddress(address, port));
    }

    /**
     * Creates a peer from a given socket address.
     *
     * @param socketAddress The socket of the peer.
     */
    public Peer(InetSocketAddress socketAddress) {
        this.socket = socketAddress;
        setupStubs();
    }


    /**
     * Creates a peer from a string representation of a socket.
     *
     * @param socket The string representation of a socket {hostname}:{port}
     * @throws MalformedSocketException Thrown when the socket is malformed.
     */
    public Peer(String socket) throws MalformedSocketException {
        this.socket = getSocketFromString(socket);
        setupStubs();
    }

    private void setupStubs() {
        this.channel = ManagedChannelBuilder
                .forAddress(socket.getHostString(), socket.getPort())
                .usePlaintext()
                .build();
        this.blockingStub = NodeGrpc.newBlockingStub(channel);
        this.asyncStub = NodeGrpc.newStub(channel);


        Runtime.getRuntime().addShutdownHook(new Thread(channel::shutdown));
    }

    public void stop() {
        this.channel.shutdown();
    }

    private InetSocketAddress getSocketFromString(String socket) throws MalformedSocketException {
        if (!socket.contains(":")) {
            throw new MalformedSocketException("Socket representation does not contain a colon separator.");
        }

        String[] socketSplit = socket.split(":");
        if (socketSplit.length != 2) {
            throw new MalformedSocketException("Socket representation does not contain a single separator.");
        }

        int socketPort;
        try {
            socketPort = Integer.parseInt(socketSplit[1]);
        } catch (NumberFormatException e) {
            throw new MalformedSocketException("Socket port section is not an integer.");
        }

        InetSocketAddress socketAddress;
        try {
            socketAddress = new InetSocketAddress(socketSplit[0], socketPort);
        } catch (IllegalArgumentException e) {
            throw new MalformedSocketException(e.getMessage());
        }

        return socketAddress;
    }

    @Override
    public String toString() {
        return String.format("%s:%d", socket.getHostString(), socket.getPort());
    }

    @Override
    public int hashCode() {
        return socket.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Peer other = (Peer) obj;
        return socket.getAddress().getHostAddress() == other.socket.getAddress().getHostAddress() &&
                socket.getPort() == other.socket.getPort();
    }
}
