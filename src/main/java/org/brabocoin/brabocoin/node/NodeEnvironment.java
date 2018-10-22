package org.brabocoin.brabocoin.node;

import net.badata.protobuf.converter.Converter;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.processor.PeerProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Represents a node environment.
 */
public class NodeEnvironment {
    protected BraboConfig config;

    protected BlockDatabase database;
    private Set<Peer> peers = new HashSet<>();
    private Converter converter = Converter.create();
    private Map<Hash, Transaction> transactionPool = new HashMap<>();

    private PeerProcessor peerProcessor;

    public NodeEnvironment(BlockDatabase database, BraboConfig config) {
        this.config = config;
        this.database = database;
        this.peerProcessor = new PeerProcessor(peers, config);
    }

    public NodeEnvironment(KeyValueStore store, BraboConfig config) throws DatabaseException {
        this.config = config;
        this.database = new BlockDatabase(store, config);
        this.peerProcessor = new PeerProcessor(peers, config);
    }

    /**
     * Setup the environment, loading the config and bootstrapping peers.
     */
    public void setup() {
        peerProcessor.bootstrap();
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
     * Handles the receival of a new block.
     * TODO: Describe logic
     *
     * @param blockHash Hash of the new block.
     */
    public void onReceiveBlockHash(@NotNull Hash blockHash) {
        // TODO: Implement and log
    }

    /**
     * Handles the receival of a new transaction.
     * TODO: Describe logic
     *
     * @param transactionHash Hash of the new block.
     */
    public void onReceiveTransaction(@NotNull Hash transactionHash) {
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
        try {
            return database.findBlock(blockHash);
        } catch (DatabaseException e) {
            // TODO: Log
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
        return null;
    }

    /**
     * Get an iterator for all transaction hashes in the transaction pool.
     *
     * @return Transaction hash iterator.
     */
    public Iterator<Hash> getTransactionIterator() {
        return transactionPool.keySet().iterator();
    }

    /**
     * Gets the height of the top block.
     *
     * @return Top block height.
     */
    public long getTopBlockHeight() {
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
