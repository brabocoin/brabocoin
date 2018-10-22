package org.brabocoin.brabocoin.processor;

import com.google.protobuf.Empty;
import io.grpc.StatusRuntimeException;
import org.brabocoin.brabocoin.exceptions.MalformedSocketException;
import org.brabocoin.brabocoin.model.messages.HandshakeResponse;
import org.brabocoin.brabocoin.node.Peer;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PeerProcessor {
    private Set<Peer> peers;
    private BraboConfig config;
    public PeerProcessor(Set<Peer> peers, BraboConfig config) {
        this.peers = peers;
        this.config = config;
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

    /**
     * Tries to handshake with bootstrapping peers until the desired number of peers are found.
     * This constant is defined in the config.
     */
    public void bootstrap() {
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
                BrabocoinProtos.HandshakeResponse protoResponse = handshakePeer.getBlockingStub()
                        .withDeadlineAfter(config.bootstrapDeadline(), TimeUnit.MILLISECONDS)
                        .handshake(Empty.newBuilder().build());
                HandshakeResponse response = ProtoConverter.toDomain(protoResponse, HandshakeResponse.Builder.class);

                // We got a response from the current handshake peer, register this peer as valid
                peers.add(handshakePeer);

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

    /**
     * Get a copy of the list of peers.
     *
     * @return List of peers.
     */
    private List<Peer> getPeers() {
        return new ArrayList<>(peers);
    }
}
