package org.brabocoin.brabocoin.node;

import com.google.protobuf.Empty;
import io.grpc.StatusRuntimeException;
import net.badata.protobuf.converter.Converter;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.exceptions.MalformedSocketException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.messages.HandshakeResponse;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Represents a node environment.
 */
public class NodeEnvironment {
    private static final Logger LOGGER = Logger.getLogger(NodeEnvironment.class.getName());
    protected BraboConfig config;

    protected BlockDatabase database;
    private Set<Peer> peers = new HashSet<>();
    private Converter converter = Converter.create();
    private Map<Hash, Transaction> transactionPool = new HashMap<>();

    public NodeEnvironment(BlockDatabase database, BraboConfig config) {
        this.config = config;
        this.database = database;
    }

    public NodeEnvironment(KeyValueStore store, BraboConfig config) throws DatabaseException {
        this.config = config;
        this.database = new BlockDatabase(store, config);
    }

    /**
     * Adds a peer to the list of peers known to this node.
     *
     * @param peer The peer to add to the peer list known to this node.
     */
    public void addPeer(Peer peer) {
        LOGGER.fine("Adding peer.");
        LOGGER.log(Level.FINEST, "peer: {0}", peer);
        peers.add(peer);
    }

    /**
     * Adds a list of peers to the list of peers known to this node.
     *
     * @param peers The peers to add to the peer list known to this node.
     */
    public void addPeers(List<Peer> peers) {
        LOGGER.fine("Adding list of peers.");
        LOGGER.log(Level.FINEST, "peers: {0}", peers.stream().map(Peer::toString).collect(Collectors.joining(", ")));
        this.peers.addAll(peers);
    }

    /**
     * Get a copy of the set of peers.
     *
     * @return Set of peers.
     */
    public Set<Peer> getPeers() {
        LOGGER.fine("Creating a copy of the set of peers.");
        return new HashSet<>(peers);
    }

    /**
     * Setup the environment, loading the config and bootstrapping peers.
     */
    public void setup() {
        LOGGER.info("Setting up the node environment.");
        bootstrap();
        LOGGER.info("Environment setup done.");
    }

    /**
     * Initializes the set of peers.
     */
    private void instantiateBootstrapPeers() {
        LOGGER.fine("Instantiating bootstrap peers.");
        for (final String peerSocket : config.bootstrapPeers()) {
            LOGGER.log(Level.FINEST, "Peer socket read from config: {0}", peerSocket);
            try {
                Peer p = new Peer(peerSocket);
                peers.add(p);
                LOGGER.log(Level.FINEST, "Peer created and added to peer set: {0}", p);
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
     * Handles the receival of a new block.
     * TODO: Describe logic
     *
     * @param blockHash Hash of the new block.
     */
    public void onReceiveBlockHash(@NotNull Hash blockHash) {
        LOGGER.fine("Block hash received.");
        LOGGER.log(Level.FINEST, "Hash: {0}", ByteUtil.toHexString(blockHash.getValue()));
        // TODO: Implement and log
    }

    /**
     * Handles the receival of a new transaction.
     * TODO: Describe logic
     *
     * @param transactionHash Hash of the new block.
     */
    public void onReceiveTransaction(@NotNull Hash transactionHash) {
        LOGGER.fine("Transaction hash received.");
        LOGGER.log(Level.FINEST, "Hash: {0}", ByteUtil.toHexString(transactionHash.getValue()));
        // TODO: Implement and log
    }

    /**
     * Handles the request for a block.
     * TODO: Describe logic
     *
     * @param blockHash Hash of the block to get.
     * @return Block instance or null if not found.
     */
    public Block getBlock(@NotNull Hash blockHash) {
        LOGGER.fine("Block requested by hash.");
        LOGGER.log(Level.FINEST, "Hash: {0}", ByteUtil.toHexString(blockHash.getValue()));
        try {
            return database.findBlock(blockHash);
        } catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Database error while trying to acquire block, error: {0}", e.getMessage());
            return null;
        }
    }

    /**
     * Handles the request for a transaction.
     * TODO: Describe logic
     *
     * @param transactionHash Hash of the transaction to get.
     * @return Transaction instance or null if not found.
     */
    public Transaction getTransaction(@NotNull Hash transactionHash) {
        LOGGER.fine("Transaction requested by hash.");
        LOGGER.log(Level.FINEST, "Hash: {0}", ByteUtil.toHexString(transactionHash.getValue()));
        return null;
    }

    /**
     * Get an iterator for all transaction hashes in the transaction pool.
     *
     * @return Transaction hash iterator.
     */
    public Iterator<Hash> getTransactionIterator() {
        LOGGER.fine("Transaction iterator creation requested.");
        return transactionPool.keySet().iterator();
    }

    /**
     * Gets the height of the top block.
     *
     * @return Top block height.
     */
    public long getTopBlockHeight() {
        LOGGER.fine("Top block height requested.");
        // TODO: magic here
        return 0;
    }

    /**
     * Check whether the given block hash is contained in the main chain.
     *
     * @param blockHash Hash of the block to check.
     * @return True if it is contained in the main chain.
     */
    public boolean isChainCompatible(@NotNull Hash blockHash) {
        LOGGER.fine("Chain compatibility check requested for block hash.");
        LOGGER.log(Level.FINEST, "Hash: {0}", ByteUtil.toHexString(blockHash.getValue()));
        // TODO: Magic here
        return false;
    }

    /**
     * Gets the block hashes above a given block hash.
     *
     * @param blockHash The block hash to indicate the starting position.
     * @return Iterator over block hashes above the given block.
     */
    public Iterator<Hash> getBlocksAbove(Hash blockHash) {
        LOGGER.fine("Block hashes requested above a given block hash.");
        LOGGER.log(Level.FINEST, "Hash: {0}", ByteUtil.toHexString(blockHash.getValue()));
        return new Iterator<Hash>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Hash next() {
                return null;
            }
        };
    }

    /**
     * TODO: Create JavaDoc
     */
    private void bootstrap() {
        LOGGER.info("Bootstrapping initiated.");
        // Populate bootstrap peers
        instantiateBootstrapPeers();
        // A list of peers for which we need to do a handshake
        List<Peer> handshakePeers = new ArrayList<>(getPeers());

        if (handshakePeers.size() <= 0) {
            LOGGER.severe("No bootstrapping peers found.");
            // TODO: What to do now?
            return;
        }

        while (getPeers().size() < config.targetPeerCount() && handshakePeers.size() > 0) {
            Peer handshakePeer = handshakePeers.remove(0);
            LOGGER.log(Level.FINEST,"Bootstrapping on peer: {0}", handshakePeer);
            try {
                LOGGER.log(Level.FINEST,"Performing handshake.");
                // Perform a handshake with the peer
                BrabocoinProtos.HandshakeResponse protoResponse = handshakePeer.getBlockingStub()
                        .withDeadlineAfter(config.bootstrapDeadline(), TimeUnit.MILLISECONDS)
                        .handshake(Empty.newBuilder().build());
                HandshakeResponse response = converter.toDomain(HandshakeResponse.Builder.class, protoResponse).build();
                LOGGER.log(Level.FINEST,"Response acquired, got {0} peers.", response.getPeers().size());

                LOGGER.log(Level.FINEST,"Adding handshake peer to peer list, as handshake was successful.");
                // We got a response from the current handshake peer, register this peer as valid
                addPeer(handshakePeer);

                // Add the discovered peers to the list of handshake peers
                for (final String peerSocket : response.getPeers()) {
                    LOGGER.log(Level.FINEST,"Discovered new peer, raw socket string: {0}", peerSocket);
                    try {
                        final Peer discoveredPeer = new Peer(peerSocket);
                        LOGGER.log(Level.FINEST,"Discovered new peer parsed: {0}", discoveredPeer);
                        handshakePeers.add(discoveredPeer);
                    } catch (MalformedSocketException e) {
                        LOGGER.log(Level.WARNING,"Error while parsing raw peer socket string: {0}", e.getMessage());
                        // TODO: Ignore and continue?
                    }
                }
            } catch (StatusRuntimeException e) {
                LOGGER.log(Level.WARNING,"Error while handshaking with peer: {0}", e.getMessage());
                // TODO: Ignore and continue?
            }
        }

        // TODO: Update peers when connection is lost.
    }
}
