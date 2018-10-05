package org.brabocoin.brabocoin.node;

import io.grpc.StatusRuntimeException;
import net.badata.protobuf.converter.Converter;
import org.brabocoin.brabocoin.exceptions.MalformedSocketException;
import org.brabocoin.brabocoin.model.HandshakeRequest;
import org.brabocoin.brabocoin.model.HandshakeResponse;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Represents a node environment.
 */
public class NodeEnvironment {
    private Set<Peer> peers = new HashSet<>();
    private BraboConfig config;
    private Converter converter = Converter.create();


    public NodeEnvironment(Boolean autoSetup) {
        if (autoSetup) {
            setup();
        }
    }

    public NodeEnvironment() {
        this(true);
    }

    /**
     * Adds a peer to the list of peers known to this node.
     *
     * @param peer The peer to add to the peer list known to this node.
     */
    public void addPeer(Peer peer) {
        peers.add(peer);
    }

    /**
     * Adds a list of peers to the list of peers known to this node.
     *
     * @param peer The peers to add to the peer list known to this node.
     */
    public void addPeers(List<Peer> peer) {
        peers.addAll(peer);
    }

    /**
     * Get a copy of the list of peers.
     *
     * @return List of peers.
     */
    public List<Peer> getPeers() {
        return new ArrayList<>(peers);
    }

    /**
     * Get a config Brabocoin Configuration Provider.
     *
     * @return Instantiated BraboConfigProvider.
     */
    public BraboConfigProvider buildConfigProvider() {
        return new BraboConfigProvider();
    }

    /**
     * Setup the environment, loading the config and bootstrapping peers.
     */
    protected void setup() {
        config = buildConfigProvider().getConfig().bind("brabo", BraboConfig.class);

        bootstrap();
    }

    /**
     * Initializes the set of peers.
     */
    private void instantiateBootstrapPeers() {
        for (final String peerSocket : config.bootstrapPeers()) {
            try {
                peers.add(new Peer(peerSocket));
            } catch (MalformedSocketException e) {
                // TODO: Handle invalid peer socket representation in the config.
                // Exit throwing an error to the user or skip this peer?
                // Definitely log this.
            }
        }
    }

    private void bootstrap() {
        // Populate bootstrap peers
        instantiateBootstrapPeers();
        // A list of peers for which we need to do a handshake
        List<Peer> handshakePeers = new ArrayList<>(getPeers());

        if (handshakePeers.size() <= 0) {
            // TODO: Log to user, we can not bootstrap without any bootstrap peers
            return;
        }

        while (getPeers().size() < config.targetPeerCount() && handshakePeers.size() > 0) {
            Peer handshakePeer = handshakePeers.remove(0);
            try {
                // Perform a handshake with the peer
                BrabocoinProtos.HandshakeResponse protoResponse = handshakePeer.blockingStub
                        .withDeadlineAfter(config.bootstrapDeadline(), TimeUnit.MILLISECONDS)
                        .handshake(converter.toProtobuf(BrabocoinProtos.HandshakeRequest.class, new HandshakeRequest()));
                HandshakeResponse response = converter.toDomain(HandshakeResponse.Builder.class, protoResponse).createHandshakeResponse();

                // We got a response from the current handshake peer, register this peer as valid
                addPeer(handshakePeer);

                // Add the discovered peers to the list of handshake peers
                for (final String peerSocket : response.getPeers()) {
                    try {
                        final Peer discoveredPeer = new Peer(peerSocket);
                        handshakePeers.add(discoveredPeer);
                    } catch (final Exception e) {
                        // Bootstrap peer returned a malformed or invalid peer
                    }
                }
            } catch (StatusRuntimeException e) {
                // TODO: handle peer errors on handshake, log
            }
        }

        // TODO: Check whether the bootstrap peers were valid.
    }
}
