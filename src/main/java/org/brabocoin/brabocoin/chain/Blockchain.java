package org.brabocoin.brabocoin.chain;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.BlockInfo;
import org.brabocoin.brabocoin.dal.BlockUndo;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Provides blockchain functionality.
 * <p>
 * Manages all blocks known to the node, and maintains the indexed chain state.
 */
public class Blockchain {

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
    private final @NotNull SetMultimap<Hash, IndexedBlock> orphanMap;

    /**
     * Initializes an empty blockchain with the given database backend.
     *
     * @param database
     *     The block database backend.
     */
    public Blockchain(@NotNull BlockDatabase database) {
        this.database = database;
        this.mainChain = new IndexedChain();
        this.orphanMap = HashMultimap.create();
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
     * @param validated
     *     Validation status.
     * @return The stored block information.
     * @throws DatabaseException
     *     When the block database is not available.
     * @see BlockDatabase#storeBlock(Block, boolean)
     */
    public @NotNull BlockInfo storeBlock(@NotNull Block block, boolean validated) throws DatabaseException {
        return database.storeBlock(block, validated);
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
            return null;
        }

        return new IndexedBlock(hash, info);
    }

    /**
     * Checks whether the given block is known to be an orphan, e.g. the parent of the block is
     * not recorded in the blockchain (either on a fork or on the main chain).
     *
     * @param block
     *     The block to check.
     * @return Whether the block is an orphan.
     */
    public boolean isOrphan(@NotNull IndexedBlock block) {
        return orphanMap.containsValue(block);
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
    public void addOrphan(@NotNull IndexedBlock block) {
        orphanMap.put(block.getBlockInfo().getPreviousBlockHash(), block);
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
    public @NotNull Set<IndexedBlock> removeOrphansOfParent(@NotNull Hash parentHash) {
        return orphanMap.removeAll(parentHash);
    }

    /**
     * Pops the top block from the main chain.
     *
     * @return The removed top block.
     * @see IndexedChain#popTopBlock()
     */
    public @Nullable IndexedBlock popTopBlock() {
        return mainChain.popTopBlock();
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
    }
}
