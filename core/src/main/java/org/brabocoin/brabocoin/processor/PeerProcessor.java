package org.brabocoin.brabocoin.processor;

import io.grpc.StatusRuntimeException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import org.brabocoin.brabocoin.exceptions.MalformedSocketException;
import org.brabocoin.brabocoin.listeners.PeerSetChangedListener;
import org.brabocoin.brabocoin.model.messages.HandshakeRequest;
import org.brabocoin.brabocoin.model.messages.HandshakeResponse;
import org.brabocoin.brabocoin.node.NetworkMessage;
import org.brabocoin.brabocoin.node.NetworkMessageListener;
import org.brabocoin.brabocoin.node.Peer;
import org.brabocoin.brabocoin.config.BraboConfig;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;

import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages all tasks related to a set of peers.
 * This includes bootstrapping and maintaining the peer set to match the desired number of peers
 * set in the config.
 */
public class PeerProcessor implements NetworkMessageListener {

    private static final Logger LOGGER = Logger.getLogger(PeerProcessor.class.getName());

    private volatile ObservableSet<Peer> peers;

    private BraboConfig config;

    private Collection<PeerSetChangedListener> peerSetChangedListeners = new ArrayList<>();

    /**
     * Create a new peer processor for a referenced set of peers and a config file.
     *
     * @param peers
     *     Set of peers to manage.
     * @param config
     *     Config to use for this processor.
     */
    public PeerProcessor(Set<Peer> peers, BraboConfig config) {
        this.peers = FXCollections.observableSet(peers);
        this.config = config;

        this.peers.addListener((SetChangeListener<Peer>)change -> {
            if (change.wasAdded()) {
                peerSetChangedListeners.forEach(l -> l.onPeerAdded(change.getElementAdded()));
            }
            else {
                peerSetChangedListeners.forEach(l -> l.onPeerRemoved(change.getElementRemoved()));
            }
        });
    }

    public ObservableSet<Peer> getPeers() {
        return peers;
    }

    /**
     * Initializes the set of peers.
     *
     * @return The peers read from config.
     */
    private synchronized List<Peer> getBootstrapPeers() {
        LOGGER.fine("Getting bootstrap peers.");
        List<Peer> configPeers = new ArrayList<>();
        for (final String peerSocket : config.getBootstrapPeers()) {
            LOGGER.log(
                Level.FINEST,
                () -> MessageFormat.format("Peer socket read from config: {0}", peerSocket)
            );
            try {
                Peer p = new Peer(peerSocket);
                configPeers.add(p);
                LOGGER.log(
                    Level.FINEST,
                    () -> MessageFormat.format("Peer created and added to peer set: {0}", p)
                );
            }
            catch (MalformedSocketException e) {
                LOGGER.log(
                    Level.WARNING,
                    "Peer socket ( {0} ) is malformed, exception message: {0}",
                    new Object[] {peerSocket, e.getMessage()}
                );
            }
        }

        return configPeers;
    }

    /**
     * Discover peers using bootstrap peers.
     */
    public synchronized void bootstrap() {
        discoverPeers(getBootstrapPeers());
    }

    /**
     * The filter to apply for every discovered peer.
     *
     * @param peer
     *     Discovered peer
     * @return True if the peer passes the filter.
     */
    protected synchronized boolean filterPeer(Peer peer) {
        boolean filterLocal = true;
        if (!config.isAllowLocalPeers()) {
            filterLocal = !peer.isLocal();
        }
        return filterLocal && !peer.getAddress().isMulticastAddress();
    }

    /**
     * Tries to handshake with bootstrapping peers until the desired number of peers are found.
     * This constant is defined in the config.
     */
    public void discoverPeers(List<Peer> handshakePeers) {
        LOGGER.info("Discovering peers initiated.");

        if (handshakePeers.size() <= 0) {
            LOGGER.info("No handshake peers found.");
            return;
        }

        while (peers.size() < config.getTargetPeerCount() && handshakePeers.size() > 0) {
            Peer handshakePeer = handshakePeers.get(0);
            LOGGER.log(Level.FINEST, "Handshaking with peer: {0}", handshakePeer);
            // Perform a handshake with the peer
            HandshakeResponse response = handshake(handshakePeer);
            if (response == null || config.getNetworkId() != response.getNetworkId()) {
                handshakePeers.remove(0);
                // Also remove from peer list (to remove unresponsive peers)
                peers.remove(handshakePeer);
                continue;
            }

            LOGGER.log(
                Level.FINEST,
                "Response acquired, got {0} peers.",
                response.getPeers().size()
            );

            LOGGER.log(
                Level.FINEST,
                "Adding handshake peer to peer list, as handshake was successful."
            );
            // We got a response from the current handshake peer, register this peer as valid
            addPeer(handshakePeer);

            // Add the discovered peers to the list of handshake peers
            for (final String peerSocket : response.getPeers()) {
                LOGGER.log(Level.FINEST, "Discovered new peer, raw socket string: {0}", peerSocket);
                try {
                    final Peer discoveredPeer = new Peer(peerSocket);
                    LOGGER.log(Level.FINEST, "Discovered new peer parsed: {0}", discoveredPeer);
                    if (!peers.contains(discoveredPeer) && filterPeer(discoveredPeer)) {
                        handshakePeers.add(discoveredPeer);
                    }
                }
                catch (MalformedSocketException e) {
                    LOGGER.log(
                        Level.WARNING,
                        "Error while parsing raw peer socket string: {0}",
                        e.getMessage()
                    );
                }
            }

            handshakePeers.remove(0);
        }
    }

    /**
     * Removes unresponsive peers, using the handshake RPC.
     */
    public void clearDeadPeers() {
        Iterator<Peer> peerIterator = peers.iterator();
        while (peerIterator.hasNext()) {
            Peer peer = peerIterator.next();
            HandshakeResponse response = handshake(peer);
            if (response == null) {
                peer.shutdown();
                LOGGER.log(Level.INFO, MessageFormat.format("Removed peer: {0}", peer));
                peerIterator.remove();
            }
        }
    }

    /**
     * Handshake with the given port, sending the local service port number.
     *
     * @param peer
     *     Peer to handshake with
     * @return HandshakeResponse object.
     */
    public HandshakeResponse handshake(Peer peer) {
        BrabocoinProtos.HandshakeResponse protoResponse;
        try {
            LOGGER.log(
                Level.FINEST,
                () -> MessageFormat.format("Handshaking with peer: {0}", peer.toString())
            );
            protoResponse = peer.getBlockingStub()
                .withDeadlineAfter(config.getHandshakeDeadline(), TimeUnit.MILLISECONDS)
                .handshake(
                    ProtoConverter.toProto(
                        new HandshakeRequest(config.getServicePort(), config.getNetworkId()),
                        BrabocoinProtos.HandshakeRequest.class
                    )
                );
        }
        catch (StatusRuntimeException e) {
            LOGGER.log(Level.WARNING, "Error while handshaking with peer: {0}", e.getMessage());
            return null;
        }
        return ProtoConverter.toDomain(protoResponse, HandshakeResponse.Builder.class);
    }

    /**
     * Get a copy of the list of peers.
     *
     * @return List of peers.
     */
    public List<Peer> copyPeersList() {
        LOGGER.fine("Creating a list copy of the set of peers.");
        return new ArrayList<>(peers);
    }

    /**
     * Get a copy of the set of peers.
     *
     * @return Set of peers.
     */
    public Set<Peer> copyPeers() {
        LOGGER.fine("Creating a list copy of the set of peers.");
        return new HashSet<>(peers);
    }

    /**
     * Get all peers matching this client address
     *
     * @param address
     *     The address to match.
     * @return The list of peers matching the address.
     */
    public synchronized List<Peer> findClientPeers(InetAddress address) {
        return peers.stream().filter(p -> p.getAddress().equals(address))
            .collect(Collectors.toList());
    }

    /**
     * Add the peer to the set of peers.
     *
     * @param peer
     *     The peer to add.
     */
    public synchronized void addPeer(Peer peer) {
        if (filterPeer(peer)) {
            peers.add(peer);
            LOGGER.log(Level.FINEST, () -> MessageFormat.format("Added client peer {0}.", peer));
        }
        else {
            LOGGER.log(
                Level.FINEST,
                () -> MessageFormat.format("Client peer {0} was a local peer.", peer)
            );
        }
    }

    /**
     * Stop and remove all peers.
     */
    public synchronized void shutdownPeers() {
        for (Peer p : peers) {
            LOGGER.log(Level.FINEST, () -> MessageFormat.format("Shutting down peer: {0}", p));
            p.shutdown();
        }

        peers.clear();
    }

    /**
     * Shutdown peer and remove from peer list.
     *
     * @param peer
     *     Peer to remove
     */
    public synchronized void shutdownPeer(Peer peer) {
        peer.shutdown();
        peers.remove(peer);
    }

    public void addPeerSetChangedListener(PeerSetChangedListener listener) {
        peerSetChangedListeners.add(listener);
    }

    public void removePeerSetChangedListener(PeerSetChangedListener listener) {
        peerSetChangedListeners.remove(listener);
    }

    /**
     * Updates the peer list by discovering peers using the current peer list and bootstrap peers.
     */
    public void updatePeers() {
        LOGGER.fine("Updating peers.");
        discoverPeers(
            new ArrayList<>(Stream.concat(
                copyPeersList().stream(),
                getBootstrapPeers().stream()
            ).collect(Collectors.toSet()))
        );
    }

    @Override
    public void onIncomingMessage(NetworkMessage message, boolean isUpdate) {
        // Add sent messages to destination peer
        if (!isUpdate) {
            List<Peer> clientPeers = findClientPeers(message.getPeer().getAddress());
            // TODO: One peer might run on different ports, adding to all...
            for (Peer peer : clientPeers) {
                peer.addIncomingMessage(message);
            }
        }
    }
}
