package org.brabocoin.brabocoin.node;

import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.processor.PeerProcessor;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Consumer;

/**
 * Represents a node environment.
 */
public class NodeEnvironment {
    private static final Logger LOGGER = Logger.getLogger(NodeEnvironment.class.getName());
    protected BraboConfig config;

    private BlockDatabase database;
    private Set<Peer> peers = new HashSet<>();
    private Map<Hash, Transaction> transactionPool;

    private PeerProcessor peerProcessor;

    public NodeEnvironment(BlockDatabase database, Map<Hash, Transaction> transactionPool, BraboConfig config) {
        this.config = config;
        this.database = database;
        this.transactionPool = transactionPool;
        this.peerProcessor = new PeerProcessor(peers, config);
    }

    public NodeEnvironment(KeyValueStore store, Map<Hash, Transaction> transactionPool, BraboConfig config) throws DatabaseException {
        this.config = config;
        this.database = new BlockDatabase(store, config);
        this.peerProcessor = new PeerProcessor(peers, config);
        this.transactionPool = transactionPool;
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
        peerProcessor.bootstrap();
        LOGGER.info("Environment setup done.");
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

    public void propagateMessage(Consumer<Peer> peerConsumer) {
        for (Peer peer : peers) {
            peerConsumer.accept(peer);
        }
        LOGGER.info("Message propagated to all peers.");
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
        return transactionPool.get(transactionHash);
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
}
