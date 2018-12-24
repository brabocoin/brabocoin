package org.brabocoin.brabocoin.node;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.brabocoin.brabocoin.exceptions.MalformedSocketException;
import org.brabocoin.brabocoin.proto.services.NodeGrpc;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A representation of an peer in the network.
 */
public class Peer {

    private static final Logger LOGGER = Logger.getLogger(Peer.class.getName());
    /**
     * The socket for this peer.
     */
    private InetSocketAddress socket;

    private ManagedChannel channel;
    private NodeGrpc.NodeBlockingStub blockingStub;
    private NodeGrpc.NodeStub asyncStub;

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
        this.socket = NetworkUtil.getSocketFromString(socket);
        setupStubs();
    }

    private static ClientInterceptor createClientCallInterceptor() {
        return new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(
                    method,
                    callOptions
                )) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        super.start(createCallListener(responseListener), headers);
                    }
                };
            }
        };
    }

    private static <RespT> SimpleForwardingClientCallListener<RespT> createCallListener(
        ClientCall.Listener<RespT> forwarder) {
        return new SimpleForwardingClientCallListener<RespT>(forwarder) {
            @Override
            public void onHeaders(Metadata headers) {
                super.onHeaders(headers);
            }

            @Override
            public void onMessage(RespT message) {
                super.onMessage(message);
            }
        };
    }


    /**
     * Check whether this peer is a connection to the local machine.
     *
     * @return Whether this peer is the local machine.
     */
    public boolean isLocal() {
        return socket.getAddress().isAnyLocalAddress() || socket.getAddress().isLoopbackAddress();
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
        try {
            this.channel = ManagedChannelBuilder
                .forAddress(socket.getHostString(), socket.getPort())
                .intercept(createClientCallInterceptor())
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

    /**
     * Close the channel to this peer.
     */
    public void shutdown() {
        LOGGER.log(Level.INFO, "Stopping peer: {0}", toSocketString());
        channel.shutdown();
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
        return String.format("%s:%d", socket.getHostString(), socket.getPort());
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
}
