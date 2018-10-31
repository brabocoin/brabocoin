package org.brabocoin.brabocoin.processor;

import io.grpc.StatusRuntimeException;
import org.brabocoin.brabocoin.exceptions.MalformedSocketException;
import org.brabocoin.brabocoin.model.messages.HandshakeRequest;
import org.brabocoin.brabocoin.model.messages.HandshakeResponse;
import org.brabocoin.brabocoin.node.Peer;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;

import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages all tasks related to a set of peers.
 * This includes bootstrapping and maintaining the peer set to match the desired number of peers set in the config.
 */
public class PeerProcessor {
    private static final Logger LOGGER = Logger.getLogger(PeerProcessor.class.getName());
    private Set<Peer> peers;
    private BraboConfig config;

    /**
     * Create a new peer processor for a referenced set of peers and a config file.
     *
     * @param peers  Set of peers to manage.
     * @param config Config to use for this processor.
     */
    public PeerProcessor(Set<Peer> peers, BraboConfig config) {
        this.peers = peers;
        this.config = config;
    }

    /**
     * Initializes the set of peers.
     *
     * @return The peers read from config.
     */
    private synchronized List<Peer> getBootstrapPeers() {
        LOGGER.fine("Instantiating bootstrap peers.");
        List<Peer> configPeers = new ArrayList<>();
        for (final String peerSocket : config.bootstrapPeers()) {
            LOGGER.log(Level.FINEST, () -> MessageFormat.format("Peer socket read from config: {0}", peerSocket));
            try {
                Peer p = new Peer(peerSocket);
                configPeers.add(p);
                LOGGER.log(Level.FINEST, () -> MessageFormat.format("Peer created and added to peer set: {0}", p));
            } catch (MalformedSocketException e) {
                LOGGER.log(Level.WARNING, "Peer socket ( {0} ) is malformed, exception message: {0}", new Object[]{
                        peerSocket, e.getMessage()
                });
                // TODO: Handle invalid peer socket representation in the config.
                // Exit throwing an error to the user or skip this peer?
            }
        }

        return configPeers;
    }

    /**
     * Tries to handshake with bootstrapping peers until the desired number of peers are found.
     * This constant is defined in the config.
     *
     * @param servicePort The service port of the local node.
     */
    public synchronized void bootstrap(int servicePort) {
        LOGGER.info("Bootstrapping initiated.");

        // A list of peers for which we need to do a handshake
        List<Peer> handshakePeers = getBootstrapPeers();

        if (handshakePeers.size() <= 0) {
            LOGGER.severe("No bootstrapping peers found.");
            // TODO: What to do now?
            return;
        }

        while (peers.size() < config.targetPeerCount() && handshakePeers.size() > 0) {
            Peer handshakePeer = handshakePeers.get(0);
            LOGGER.log(Level.FINEST, "Bootstrapping on peer: {0}", handshakePeer);
            try {
                LOGGER.log(Level.FINEST, "Performing handshake.");
                // Perform a handshake with the peer
                BrabocoinProtos.HandshakeResponse protoResponse = handshakePeer.getBlockingStub()
                        .withDeadlineAfter(config.bootstrapDeadline(), TimeUnit.MILLISECONDS)
                        .handshake(
                                ProtoConverter.toProto(
                                        new HandshakeRequest(servicePort), BrabocoinProtos.HandshakeRequest.class
                                )
                        );
                HandshakeResponse response = ProtoConverter.toDomain(protoResponse, HandshakeResponse.Builder.class);
                LOGGER.log(Level.FINEST, "Response acquired, got {0} peers.", response.getPeers().size());

                LOGGER.log(Level.FINEST, "Adding handshake peer to peer list, as handshake was successful.");
                // We got a response from the current handshake peer, register this peer as valid
                peers.add(handshakePeer);

                // Add the discovered peers to the list of handshake peers
                for (final String peerSocket : response.getPeers()) {
                    LOGGER.log(Level.FINEST, "Discovered new peer, raw socket string: {0}", peerSocket);
                    try {
                        final Peer discoveredPeer = new Peer(peerSocket);
                        LOGGER.log(Level.FINEST, "Discovered new peer parsed: {0}", discoveredPeer);
                        if (!peers.contains(discoveredPeer)) {
                            // TODO: Help? tests will fail with: && !discoveredPeer.isLocal()) {
                            handshakePeers.add(discoveredPeer);
                        }
                    } catch (MalformedSocketException e) {
                        LOGGER.log(Level.WARNING, "Error while parsing raw peer socket string: {0}", e.getMessage());
                        // TODO: Ignore and continue?
                    }
                }
            } catch (StatusRuntimeException e) {
                LOGGER.log(Level.WARNING, "Error while handshaking with peer: {0}", e.getMessage());
                // TODO: Ignore and continue?
            }

            handshakePeers.remove(0);
        }

        // TODO: Update peers when connection is lost.
    }

    /**
     * Get a copy of the list of peers.
     *
     * @return List of peers.
     */
    public synchronized List<Peer> copyPeersList() {
        LOGGER.fine("Creating a list copy of the set of peers.");
        return new ArrayList<>(peers);
    }

    /**
     * Get a copy of the set of peers.
     *
     * @return Set of peers.
     */
    public synchronized Set<Peer> copyPeers() {
        LOGGER.fine("Creating a list copy of the set of peers.");
        return new HashSet<>(peers);
    }

    /**
     * Get all peers matching this client address
     *
     * @param address The address to match.
     * @return The list of peers matching the address.
     */
    public synchronized List<Peer> findClientPeers(InetAddress address) {
        return peers.stream().filter(p -> p.getAddress().equals(address))
                .collect(Collectors.toList());
    }

    /**
     * Add the peer to the set of peers.
     *
     * @param peer The peer to add.
     */
    public synchronized void addPeer(Peer peer) {
        peers.add(peer);
    }
}
