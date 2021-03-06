package org.brabocoin.brabocoin.chain;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.RejectedBlock;
import org.brabocoin.brabocoin.model.dal.BlockInfo;
import org.brabocoin.brabocoin.model.dal.BlockUndo;
import org.brabocoin.brabocoin.validation.consensus.Consensus;
import org.brabocoin.brabocoin.validation.block.BlockValidationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import static org.brabocoin.brabocoin.util.ByteUtil.toHexString;

/**
 * Provides blockchain functionality.
 * <p>
 * Manages all blocks known to the node, and maintains the indexed chain state.
 */
public class Blockchain {

    private static final Logger LOGGER = Logger.getLogger(Blockchain.class.getName());

    /**
     * Listeners for blockchain events.
     */
    private final @NotNull Set<BlockchainListener> listeners;

    /**
     * Full block database.
     */
    private final @NotNull BlockDatabase database;

    /**
     * Contains the information of all blocks on the main chain, such that these blocks are
     * readily available.
     */
    private final @NotNull IndexedChain mainChain;

    /**
     * Stores orphan blocks indexed by their parent block hash.
     */
    private final @NotNull SetMultimap<Hash, Block> orphanMap;

    /**
     * Index orphans by their hash.
     */
    private final @NotNull Map<Hash, Block> orphanIndex;

    /**
     * The maximum number of orphans.
     */
    private final int maxOrphans;

    /**
     * Recently rejected blocks.
     */
    private final @NotNull Queue<RejectedBlock> recentRejects;

    /**
     * The random instance.
     */
    private final @NotNull Random random;

    /**
     * Initializes a blockchain with a genesis block with the given database backend.
     *
     * @param database
     *     The block database backend.
     * @param consensus
     *     The consensus on which this blockchain needs to be constructed.
     * @param maxOrphans
     *     The maximum number of orphans to store.
     * @param maxRecentRejects
     *     The maximum number of recently rejected blocks to store.
     * @param random
     *     The random instance.
     * @throws DatabaseException
     *     When the genesis block could not be stored.
     */
    public Blockchain(@NotNull BlockDatabase database,
                      @NotNull Consensus consensus, int maxOrphans,
                      int maxRecentRejects, @NotNull Random random) throws DatabaseException {
        this.database = database;
        this.orphanMap = HashMultimap.create();
        this.orphanIndex = new HashMap<>();
        this.maxOrphans = maxOrphans;
        this.recentRejects = EvictingQueue.create(maxRecentRejects);
        this.random = random;
        this.listeners = new HashSet<>();

        IndexedBlock genesis = storeGenesisBlock(consensus.getGenesisBlock());
        this.mainChain = new IndexedChain(genesis);
    }

    private @NotNull IndexedBlock storeGenesisBlock(
        @NotNull Block genesisBlock) throws DatabaseException {
        LOGGER.info("Initializing blockchain with genesis block.");
        database.storeBlock(genesisBlock, false);
        IndexedBlock indexedGenesis = getIndexedBlock(genesisBlock.getHash());
        if (indexedGenesis == null) {
            LOGGER.severe("Genesis block could not be stored.");
            throw new DatabaseException("Genesis block could not be stored.");
        }

        return indexedGenesis;
    }

    /**
     * Add a listener to blockchain events.
     *
     * @param listener The listener to add.
     */
    public void addListener(@NotNull BlockchainListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Remove a listener to blockchain events.
     *
     * @param listener The listener to remove.
     */
    public void removeListener(@NotNull BlockchainListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Get the indexed main chain (leading fork) of the blockchain.
     *
     * @return The main chain.
     */
    public @NotNull IndexedChain getMainChain() {
        return mainChain;
    }

    /**
     * Get the full block from an indexed block.
     *
     * @param block
     *     The indexed version of the full block to retrieve.
     * @return The full block from the block database, or {@code null} if the block is unknown.
     * @throws DatabaseException
     *     When the block database is not available.
     */
    public @Nullable Block getBlock(@NotNull IndexedBlock block) throws DatabaseException {
        return getBlock(block.getHash());
    }

    /**
     * Get the full block from its hash.
     *
     * @param hash
     *     The hash of the full block to retrieve.
     * @return The full block from the block database, or {@code null} if the block is unknown.
     * @throws DatabaseException
     *     When the block database is not available.
     */
    public @Nullable Block getBlock(@NotNull Hash hash) throws DatabaseException {
        return database.findBlock(hash);
    }

    /**
     * Checks whether the block with the given hash is stored in the block database.
     * <p>
     * This only checks whether the full block data is known and stored on disk, not whether this
     * block is present in the blockchain or main chain.
     *
     * @param hash
     *     The hash of the block.
     * @return Whether the block is known and stored in the database.
     * @throws DatabaseException
     *     When the block database is not available.
     */
    public boolean isBlockStored(Hash hash) throws DatabaseException {
        return database.hasBlock(hash);
    }

    /**
     * Stores a block on disk.
     *
     * @param block
     *     The block to store
     * @param minedByMe
     *     Whether this block was mined by this user.
     * @return The stored block information.
     * @throws DatabaseException
     *     When the block database is not available.
     * @see BlockDatabase#storeBlock(Block, boolean)
     */
    public BlockInfo storeBlock(@NotNull Block block, boolean minedByMe) throws DatabaseException {
        return database.storeBlock(block, minedByMe);
    }

    /**
     * Sets the status of this block as invalid.
     *
     * @param blockHash
     *     The hash of the block.
     * @throws DatabaseException
     *     When the block database is not available.
     * @see BlockDatabase#setBlockInvalid(Hash)
     */
    public void setBlockInvalid(@NotNull Hash blockHash) throws DatabaseException {
        database.setBlockInvalid(blockHash);
    }

    /**
     * Retrieve the block information from the database from the given block hash and return an
     * indexed representation of the block (for memory storage).
     *
     * @param hash
     *     The block hash.
     * @return The indexed block, or {@code null} if the block with the given hash is unknown.
     * @throws DatabaseException
     *     When the block database is not available.
     */
    public @Nullable IndexedBlock getIndexedBlock(@NotNull Hash hash) throws DatabaseException {
        BlockInfo info = database.findBlockInfo(hash);

        if (info == null) {
            LOGGER.fine("Indexed block could not be found.");
            return null;
        }

        return new IndexedBlock(hash, info);
    }

    /**
     * Checks whether the given block is known to be an orphan, e.g. the parent of the block is
     * not recorded in the blockchain (either on a fork or on the main chain).
     *
     * @param blockHash
     *     The hash of the block to check.
     * @return Whether the block is an orphan.
     */
    public synchronized boolean isOrphan(@NotNull Hash blockHash) {
        LOGGER.fine("Check if block is orphan.");

        boolean isOrphan = orphanIndex.containsKey(blockHash);
        LOGGER.finest(() -> MessageFormat.format(
            "Block {0} isOrphan={1}",
            toHexString(blockHash.getValue()),
            isOrphan
        ));

        return isOrphan;
    }

    /**
     * Add the given block as an orphan block.
     * <p>
     * The block is added to the orphan map, indexed by the hash of the parent block. These
     * blocks are stored in memory and are added to the blockchain if all ancestors of the block
     * can be added to the blockchain at some later point in time.
     *
     * @param block
     *     The block to add as orphan.
     */
    public synchronized void addOrphan(@NotNull Block block) {
        if (orphanIndex.containsKey(block.getHash())) {
            LOGGER.fine("Adding block as orphan, but is already present in orphan index.");
            return;
        }

        LOGGER.fine("Adding block as orphan.");
        orphanMap.put(block.getPreviousBlockHash(), block);
        orphanIndex.put(block.getHash(), block);

        listeners.forEach(l -> l.onOrphanAdded(block));

        // Remove random orphan when size limit is reached
        while (orphanIndex.size() > maxOrphans) {
            Hash key = getRandomOrphanHash();
            Block removed = orphanIndex.remove(key);
            orphanMap.remove(removed.getPreviousBlockHash(), removed);

            LOGGER.finest("Discarded random orphan block because maximum orphan index size was reached.");

            listeners.forEach(l -> l.onOrphanRemoved(removed));
        }
    }

    private Hash getRandomOrphanHash() {
        int index = random.nextInt(orphanIndex.size());
        Iterator<Hash> keys = orphanIndex.keySet().iterator();

        while (index > 0) {
            index--;
            keys.next();
        }

        return keys.next();
    }

    /**
     * Add a block to the recently rejected memory storage.
     *
     * @param block
     *     The block that was rejected.
     * @param validationResult
     * The result of the validation of the block.
     *
     */
    public synchronized void addRejected(@NotNull Block block,
                                         @NotNull BlockValidationResult validationResult) {
        RejectedBlock reject = new RejectedBlock(block, validationResult);
        if (recentRejects.contains(reject)) {
            return;
        }
        recentRejects.add(reject);
        listeners.forEach(l -> l.onRecentRejectAdded(block));
    }

    /**
     * Iterator over the recently rejected blocks.
     *
     * @return An iterator of the recently rejected blocks.
     */
    public Iterator<RejectedBlock> recentRejectsIterator() {
        return recentRejects.iterator();
    }

    /**
     * Iterator over the orphan blocks.
     *
     * @return An iterator of the orphan blocks.
     */
    public Iterator<Block> orphansIterator() {
        return orphanIndex.values().iterator();
    }

    /**
     * Find the block undo data.
     *
     * @param hash
     *     The hash of the block.
     * @return The block undo data, or {@code null} if the block undo data could not be found.
     * @throws DatabaseException
     *     When the block database is not available.
     * @see BlockDatabase#findBlockUndo(Hash)
     */
    public @Nullable BlockUndo findBlockUndo(Hash hash) throws DatabaseException {
        return database.findBlockUndo(hash);
    }

    /**
     * Stores the block undo data in the database.
     *
     * @param block
     *     The block to store the undo data for.
     * @param undo
     *     The undo data.
     * @throws DatabaseException
     *     When the block database is not available.
     * @see BlockDatabase#storeBlockUndo(IndexedBlock, BlockUndo)
     */
    public void storeBlockUndo(IndexedBlock block, BlockUndo undo) throws DatabaseException {
        database.storeBlockUndo(block, undo);
    }

    /**
     * Remove and return all the orphans from the orphan set which have the given hash as parent
     * block.
     *
     * @param parentHash
     *     The hash of the parent.
     * @return The set of orphan block that are removed, or an empty set if no orphans are removed.
     */
    public synchronized @NotNull Set<Block> removeOrphansOfParent(@NotNull Hash parentHash) {
        LOGGER.fine("Removing all orphans of parent.");
        Set<Block> removed = orphanMap.removeAll(parentHash);

        for (Block removedBlock : removed) {
            orphanIndex.remove(removedBlock.getHash());

            listeners.forEach(l -> l.onOrphanRemoved(removedBlock));
        }

        return removed;
    }

    /**
     * Pops the top block from the main chain.
     *
     * @return The removed top block.
     * @throws IllegalStateException
     *     When the current top block is the genesis block.
     * @see IndexedChain#popTopBlock()
     */
    public IndexedBlock popTopBlock() throws IllegalStateException {
        IndexedBlock block = mainChain.popTopBlock();

        // Notify listeners
        listeners.forEach(l -> l.onTopBlockDisconnected(block));

        return block;
    }

    /**
     * Pushes the top block on the main chain.
     *
     * @param block
     *     The block to add.
     * @see IndexedChain#pushTopBlock(IndexedBlock)
     */
    public void pushTopBlock(@NotNull IndexedBlock block) {
        mainChain.pushTopBlock(block);

        // Notify listeners
        listeners.forEach(l -> l.onTopBlockConnected(block));
    }

    /**
     * Get the orphan, given a hash.
     *
     * @param blockHash
     *     The orphan block hash to find the block for
     * @return Orphan block
     */
    public @Nullable Block getOrphan(@NotNull Hash blockHash) {
        return orphanIndex.get(blockHash);
    }
}
