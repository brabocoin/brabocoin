package org.brabocoin.brabocoin.processor;

import com.google.protobuf.Empty;
import io.grpc.StatusRuntimeException;
import org.brabocoin.brabocoin.exceptions.MalformedSocketException;
import org.brabocoin.brabocoin.model.messages.HandshakeResponse;
import org.brabocoin.brabocoin.node.Peer;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     */
    private void instantiateBootstrapPeers() {
        LOGGER.fine("Instantiating bootstrap peers.");
        for (final String peerSocket : config.bootstrapPeers()) {
            LOGGER.log(Level.FINEST, () -> MessageFormat.format("Peer socket read from config: {0}", peerSocket));
            try {
                Peer p = new Peer(peerSocket);
                peers.add(p);
                LOGGER.log(Level.FINEST, () -> MessageFormat.format("Peer created and added to peer set: {0}", p));
            } catch (MalformedSocketException e) {
                LOGGER.log(Level.WARNING, "Peer socket ( {0} ) is malformed, exception message: {0}", new Object[]{
                        peerSocket, e.getMessage()
                });
                // TODO: Handle invalid peer socket representation in the config.
                // Exit throwing an error to the user or skip this peer?
            }
        }
    }

    /**
     * Tries to handshake with bootstrapping peers until the desired number of peers are found.
     * This constant is defined in the config.
     */
    public void bootstrap() {
        LOGGER.info("Bootstrapping initiated.");
        // Populate bootstrap peers
        instantiateBootstrapPeers();
        // A list of peers for which we need to do a handshake
        List<Peer> handshakePeers = copyPeers();

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
                        .handshake(Empty.newBuilder().build());
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
    private List<Peer> copyPeers() {
        LOGGER.fine("Creating a list copy of the set of peers.");
        return new ArrayList<>(peers);
    }
}
