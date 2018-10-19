package org.brabocoin.brabocoin.chain;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.BlockInfo;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.utxo.UTXOSet;
import org.brabocoin.brabocoin.validation.BlockValidator;
import org.brabocoin.brabocoin.validation.Consensus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
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
     * UTXO set.
     */
    private final @NotNull UTXOSet utxoSet;

    /**
     * Block validator.
     */
    private final @NotNull BlockValidator blockValidator;

    /**
     * Contains the information of all blocks on the main mainChain, such that these blocks are
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
     * @param utxoSet
     *     The UTXO set.
     * @param blockValidator
     *     The block validator.
     */
    public Blockchain(@NotNull BlockDatabase database, @NotNull UTXOSet utxoSet,
                      @NotNull BlockValidator blockValidator) {
        this.database = database;
        this.utxoSet = utxoSet;
        this.blockValidator = blockValidator;
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
    public @Nullable Block getBlock(IndexedBlock block) throws DatabaseException {
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
    public @Nullable Block getBlock(Hash hash) throws DatabaseException {
        return database.findBlock(hash);
    }

    /**
     * Add a block to the blockchain.
     * <p>
     * Validates and stores the block data to the block database. Note that invalid blocks are
     * discarded and not stored.
     * <p>
     * New blocks are added to the block chain if possible. Any orphans that are descendants of
     * the new block are also added, if able. When a new fork becomes leading, the main chain is
     * reorganized and the chain state is updated.
     *
     * @param block
     *     The block to add.
     * @return The status of the block that is added.
     * @throws DatabaseException
     *     When the block database is not available.
     */
    public NewBlockStatus processNewBlock(@NotNull Block block) throws DatabaseException {
        // Check if the block is valid
        if (!checkBlockValid(block)) {
            return NewBlockStatus.INVALID;
        }

        // Check if we already have the block
        Hash hash = block.computeHash();
        if (isBlockStored(hash)) {
            return NewBlockStatus.ALREADY_STORED;
        }

        // Store the block on disk
        BlockInfo info = database.storeBlock(block, true);
        IndexedBlock indexedBlock = new IndexedBlock(hash, info);

        // Find the parent block
        IndexedBlock parent = getIndexedBlock(block.getPreviousBlockHash());

        // If parent is unknown or orphan, this block is an orphan as well
        if (parent == null || isOrphan(parent)) {
            addOrphan(indexedBlock);
            return NewBlockStatus.ORPHAN;
        }

        // Check if any orphan blocks are descendants and can be added as well
        // Return the leaf blocks of the new family, which are new tip candidates
        Set<IndexedBlock> tipCandidates = processOrphansTipCandidates(indexedBlock);

        // Update the main chain if necessary
        updateMainChain(tipCandidates);

        return NewBlockStatus.ADDED_TO_BLOCKCHAIN;
    }

    /**
     * Checks whether a block is valid.
     *
     * @param block
     *     The block to check.
     * @return Whether the block is valid.
     * @see BlockValidator
     */
    private boolean checkBlockValid(@NotNull Block block) {
        return blockValidator.checkBlockValid(block);
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
     * Retrieve the block information from the database from the given block hash and return an
     * indexed representation of the block (for memory storage).
     *
     * @param hash
     *     The block hash.
     * @return The indexed block, or {@code null} if the block with the given hash is unknown.
     * @throws DatabaseException
     *     When the block database is not available.
     */
    private @Nullable IndexedBlock getIndexedBlock(@NotNull Hash hash) throws DatabaseException {
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
    private boolean isOrphan(@NotNull IndexedBlock block) {
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
    private void addOrphan(@NotNull IndexedBlock block) {
        orphanMap.put(block.getBlockInfo().getPreviousBlockHash(), block);
    }

    /**
     * Find from the pool of orphan blocks any blocks that are descendants of the given block and
     * remove them from the orphan pool.
     * <p>
     * From the set of blocks that are removed as orphans, return the blocks that have no further
     * descendants from the orphan pool. These blocks are now new candidates for the tip of the
     * main chain.
     *
     * @param newParent
     *     The new parent to find descendants for.
     * @return The set of new tip candidates from the orphan pool (that are now removed as
     * orphans and are added to the blockchain).
     */
    private @NotNull Set<IndexedBlock> processOrphansTipCandidates(@NotNull IndexedBlock newParent) {
        Set<IndexedBlock> tipCandidates = new HashSet<>();

        Deque<IndexedBlock> queue = new ArrayDeque<>();
        queue.add(newParent);

        while (!queue.isEmpty()) {
            IndexedBlock orphan = queue.remove();

            // Find any orphan descendants and mark them as part of the chain
            Set<IndexedBlock> descendants = orphanMap.removeAll(orphan.getHash());

            if (descendants.isEmpty()) {
                // When this orphan has no more known descendants, make tip candidate
                // TODO: maybe check if it actually supersedes the current tip?
                tipCandidates.add(orphan);
            } else {
                // Add the removed blocks to the queue to find further descendant orphans
                queue.addAll(descendants);
            }
        }

        return tipCandidates;
    }

    /**
     * Update the main chain by selecting the new best tip from the provided tip candidates.
     * <p>
     * If none of the tip candidates is better than the current tip, nothing will happen. When a
     * new tip is selected, the main chain is reorganized.
     *
     * @param tipCandidates
     *     The set of possible new tip blocks.
     * @throws DatabaseException
     *     When the blocks database is not available.
     */
    private void updateMainChain(Set<IndexedBlock> tipCandidates) throws DatabaseException {
        // Add the current tip to the tip candidates
        IndexedBlock currentTip = mainChain.getTipBlock();
        Set<IndexedBlock> allCandidates = new HashSet<>(tipCandidates);
        allCandidates.add(currentTip);

        // Select the best tip candidate
        // TODO: Re-organize on block height tiebreaker??
        IndexedBlock bestCandidate = Consensus.bestBlock(allCandidates);

        // If tip does not change, do nothing
        if (bestCandidate == null || bestCandidate.equals(currentTip)) {
            return;
        }

        // Main chain needs to be updated: find the fork that needs to become active
        Deque<IndexedBlock> fork = findFork(bestCandidate);
        if (fork == null) {
            // TODO: need rollback when find fork fails?
            return;
        }

        // Get block at up to which the main chain needs to be reverted
        IndexedBlock revertTargetBlock = fork.pop();

        // Revert chain state to target block by disconnecting tip blocks
        while (!revertTargetBlock.equals(mainChain.getTipBlock())) {
            disconnectTip();
        }

        // Connect the blocks in the new fork
        while (!fork.isEmpty()) {
            IndexedBlock newBlock = fork.pop();
            connectTip(newBlock);
        }
    }

    /**
     * Find all blocks present on the fork off the main chain up to the given block.
     * <p>
     * Backtrack from the given block through all parents to the first block present on the main
     * chain. The given block, all intermediate parent blocks and the first block present on the
     * main chain are recorded in-order and returned.
     * <p>
     * Note that these blocks are not indexed in memory and are first retrieved from the database.
     *
     * @param block
     *     The block to find the fork to the main chain for.
     * @return All the blocks present on the fork, in-order, up to and including the first block
     * on the main chain, or {@code null} when no path back to the main chain could be found.
     * @throws DatabaseException
     *     When the block database is not available.
     */
    private @Nullable Deque<IndexedBlock> findFork(@NotNull IndexedBlock block) throws DatabaseException {
        Deque<IndexedBlock> fork = new ArrayDeque<>();
        IndexedBlock parent = block;

        // Backtrack to the main chain, loading previous blocks from the database
        while (parent != null && !mainChain.contains(parent)) {
            fork.push(parent);
            parent = getIndexedBlock(parent.getBlockInfo().getPreviousBlockHash());
        }

        // If no fork is found, return null
        if (parent == null) {
            return null;
        }

        // Add the fork block in the main chain as well so we know where to update
        fork.push(parent);
        return fork;
    }

    /**
     * Disconnects the current tip block from the main chain, while updating the chain state to
     * account for the changed main chain.
     * <p>
     * The tip is removed from the main chain, and the UTXO set and transaction pool are updated
     * accordingly.
     *
     * @throws DatabaseException
     *     When the blocks database is not available.
     */
    private void disconnectTip() throws DatabaseException {
        IndexedBlock tip = mainChain.getTipBlock();

        if (tip == null) {
            return;
        }

        // Read block from disk
        Block block = database.findBlock(tip.getHash());
        assert block != null;

        // Update UTXO set
        utxoSet.processBlockDisconnected(block);

        // TODO: update mempool

        // Set the new tip to the parent of the previous tip
        mainChain.popTipBlock();
    }

    /**
     * Connects the given block as the new tip to the main chain, while updating the chain state
     * to account for the changed main chain.
     * <p>
     * The block is added to the main chain, and the UTXO set and transaction pool are updated
     * accordingly.
     *
     * @param tip
     *     The block to be added as the new tip.
     * @throws DatabaseException
     *     When the blocks database is not available.
     */
    private void connectTip(@NotNull IndexedBlock tip) throws DatabaseException {
        // Read block from disk
        Block block = database.findBlock(tip.getHash());
        assert block != null;

        utxoSet.processBlockConnected(block);

        // TODO: remove transactions from mempool

        // Set the new tip in the main chain
        mainChain.pushTipBlock(tip);
    }

    /**
     * The result of processing a new block.
     */
    public enum NewBlockStatus {

        /**
         * The block is added as orphan because not all ancestors of the block are already known.
         */
        ORPHAN,

        /**
         * The new block is successfully added to the blockchain, either on the main chain or as
         * a fork.
         */
        ADDED_TO_BLOCKCHAIN,

        /**
         * The block has already been processed and stored.
         */
        ALREADY_STORED,

        /**
         * The block is invalid and has not been stored.
         */
        INVALID
    }
}
