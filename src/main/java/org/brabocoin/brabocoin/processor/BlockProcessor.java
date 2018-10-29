package org.brabocoin.brabocoin.processor;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.dal.BlockInfo;
import org.brabocoin.brabocoin.model.dal.BlockUndo;
import org.brabocoin.brabocoin.validation.BlockValidator;
import org.brabocoin.brabocoin.validation.Consensus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static org.brabocoin.brabocoin.util.ByteUtil.toHexString;

/**
 * Processes a new incoming block.
 */
public class BlockProcessor {

    private static final Logger LOGGER = Logger.getLogger(BlockProcessor.class.getName());

    /**
     * UTXO processor.
     */
    private final @NotNull UTXOProcessor utxoProcessor;

    /**
     * Transaction processor.
     */
    private final @NotNull TransactionProcessor transactionProcessor;

    /**
     * Block validator.
     */
    private final @NotNull BlockValidator blockValidator;

    /**
     * The consensus.
     */
    private final @NotNull Consensus consensus;

    /**
     * The blockchain.
     */
    private final @NotNull Blockchain blockchain;

    /**
     * Creates a new block processor.
     *
     * @param blockchain
     *     The blockchain.
     * @param utxoProcessor
     *     The UTXO processor.
     * @param transactionProcessor
     *     The transaction processor.
     * @param consensus
     *     The consensus on which to process blocks.
     * @param blockValidator
     *     The block validator.
     */
    public BlockProcessor(@NotNull Blockchain blockchain, @NotNull UTXOProcessor utxoProcessor,
                          @NotNull TransactionProcessor transactionProcessor,
                          @NotNull Consensus consensus, @NotNull BlockValidator blockValidator) {
        LOGGER.fine("Initializing BlockProcessor.");
        this.blockchain = blockchain;
        this.utxoProcessor = utxoProcessor;
        this.transactionProcessor = transactionProcessor;
        this.consensus = consensus;
        this.blockValidator = blockValidator;
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
    public ProcessedBlockStatus processNewBlock(@NotNull Block block) throws DatabaseException {
        LOGGER.fine("Processing new block.");

        // Check if the block is valid
        if (!checkBlockValid(block)) {
            LOGGER.info("New block is invalid.");
            return ProcessedBlockStatus.INVALID;
        }

        // Check if we already have the block
        Hash hash = block.computeHash();
        if (blockchain.isBlockStored(hash)) {
            LOGGER.info("New block was already stored.");
            return ProcessedBlockStatus.ALREADY_STORED;
        }

        // Store the block on disk
        BlockInfo info = blockchain.storeBlock(block, true);
        IndexedBlock indexedBlock = new IndexedBlock(hash, info);

        // Find the parent block
        IndexedBlock parent = blockchain.getIndexedBlock(block.getPreviousBlockHash());

        // If parent is unknown or orphan, this block is an orphan as well
        if (parent == null || blockchain.isOrphan(parent)) {
            blockchain.addOrphan(indexedBlock);
            LOGGER.info("New block is added as orphan.");
            return ProcessedBlockStatus.ORPHAN;
        }

        // Check if any orphan blocks are descendants and can be added as well
        // Return the leaf blocks of the new family, which are new top candidates
        Set<IndexedBlock> topCandidates = processOrphansTopCandidates(indexedBlock);

        // Update the main chain if necessary
        updateMainChain(topCandidates);

        LOGGER.info("New block is added to the blockchain.");
        return ProcessedBlockStatus.ADDED_TO_BLOCKCHAIN;
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
     * Find from the pool of orphan blocks any blocks that are descendants of the given block and
     * remove them from the orphan pool.
     * <p>
     * From the set of blocks that are removed as orphans, return the blocks that have no further
     * descendants from the orphan pool. These blocks are now new candidates for the top of the
     * main chain.
     *
     * @param newParent
     *     The new parent to find descendants for.
     * @return The set of new top candidates from the orphan pool (that are now removed as
     * orphans and are added to the blockchain).
     */
    private @NotNull Set<IndexedBlock> processOrphansTopCandidates(@NotNull IndexedBlock newParent) {
        LOGGER.fine("Processing orphans for top candidates.");

        Set<IndexedBlock> topCandidates = new HashSet<>();

        Deque<IndexedBlock> queue = new ArrayDeque<>();
        queue.add(newParent);

        while (!queue.isEmpty()) {
            IndexedBlock orphan = queue.remove();

            // Find any orphan descendants and mark them as part of the chain
            Set<IndexedBlock> descendants = blockchain.removeOrphansOfParent(orphan.getHash());

            if (descendants.isEmpty()) {
                // When this orphan has no more known descendants, make top candidate
                // TODO: maybe check if it actually supersedes the current top?
                topCandidates.add(orphan);
                LOGGER.finest(() -> MessageFormat.format(
                    "Added orphan {0} as top candidate.",
                    toHexString(orphan.getHash().getValue())
                ));
            } else {
                // Add the removed blocks to the queue to find further descendant orphans
                queue.addAll(descendants);
            }
        }

        return topCandidates;
    }

    /**
     * Update the main chain by selecting the new best top from the provided top candidates.
     * <p>
     * If none of the top candidates is better than the current top, nothing will happen. When a
     * new top is selected, the main chain is reorganized.
     *
     * @param topCandidates
     *     The set of possible new top blocks.
     * @throws DatabaseException
     *     When the blocks database is not available.
     */
    private void updateMainChain(Set<IndexedBlock> topCandidates) throws DatabaseException {
        LOGGER.fine("Updating main chain.");

        // Add the current top to the top candidates
        IndexedBlock currentTop = blockchain.getMainChain().getTopBlock();
        Set<IndexedBlock> allCandidates = new HashSet<>(topCandidates);
        allCandidates.add(currentTop);

        // Select the best top candidate
        IndexedBlock bestCandidate = consensus.bestBlock(allCandidates);

        // If top does not change, do nothing
        if (bestCandidate == null || bestCandidate.equals(currentTop)) {
            LOGGER.fine("Main chain is not updated.");
            return;
        }

        // Main chain needs to be updated: find the fork that needs to become active
        Deque<IndexedBlock> fork = findFork(bestCandidate);
        if (fork == null) {
            // TODO: need rollback when find fork fails?
            LOGGER.severe("Main chain needs to be updated, but no fork is found.");
            return;
        }

        // Get block at up to which the main chain needs to be reverted
        IndexedBlock revertTargetBlock = fork.pop();
        LOGGER.finest(() -> MessageFormat.format(
            "Target block to revert main chain to {0}.",
            toHexString(revertTargetBlock.getHash().getValue())
        ));

        // Revert chain state to target block by disconnecting top blocks
        while (!revertTargetBlock.equals(blockchain.getMainChain().getTopBlock())) {
            disconnectTop();
        }

        // Connect the blocks in the new fork
        while (!fork.isEmpty()) {
            IndexedBlock newBlock = fork.pop();
            connectTopBlock(newBlock);
        }

        LOGGER.info("Main chain is updated with new top block.");
        LOGGER.finest(MessageFormat.format(
            "New top block {0}.",
            toHexString(blockchain.getMainChain().getTopBlock().getHash().getValue())
        ));
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
        LOGGER.fine("Find a fork.");

        Deque<IndexedBlock> fork = new ArrayDeque<>();
        IndexedBlock parent = block;

        // Backtrack to the main chain, loading previous blocks from the database
        while (parent != null && !blockchain.getMainChain().contains(parent)) {
            fork.push(parent);
            parent = blockchain.getIndexedBlock(parent.getBlockInfo().getPreviousBlockHash());
        }

        // If no fork is found, return null
        if (parent == null) {
            LOGGER.fine("No fork is found.");
            return null;
        }

        // Add the fork block in the main chain as well so we know where to update
        fork.push(parent);
        return fork;
    }

    /**
     * Disconnects the current top block from the main chain, while updating the chain state to
     * account for the changed main chain.
     * <p>
     * The top is removed from the main chain, and the UTXO set and transaction pool are updated
     * accordingly.
     *
     * @throws DatabaseException
     *     When the blocks database is not available.
     */
    private void disconnectTop() throws DatabaseException {
        IndexedBlock top = blockchain.getMainChain().getTopBlock();

        Hash hash = top.getHash();
        LOGGER.finest(() -> MessageFormat.format(
            "Disconnecting block {0}",
            toHexString(hash.getValue())
        ));

        // Read block from disk
        Block block = blockchain.getBlock(hash);
        assert block != null;

        // Read undo data from disk
        BlockUndo blockUndo = blockchain.findBlockUndo(hash);
        assert blockUndo != null;

        // Update UTXO set
        utxoProcessor.processBlockDisconnected(block, blockUndo);

        // Update transaction pool
        transactionProcessor.processTopBlockDisconnected(block);

        // Set the new top to the parent of the previous top
        blockchain.popTopBlock();
    }

    /**
     * Connects the given block as the new top to the main chain, while updating the chain state
     * to account for the changed main chain.
     * <p>
     * The block is added to the main chain, and the UTXO set and transaction pool are updated
     * accordingly.
     *
     * @param top
     *     The block to be added as the new top.
     * @throws DatabaseException
     *     When the blocks database is not available.
     */
    private void connectTopBlock(@NotNull IndexedBlock top) throws DatabaseException {
        LOGGER.finest(() -> MessageFormat.format(
            "Connecting block {0}",
            toHexString(top.getHash().getValue())
        ));

        // Read block from disk
        Block block = blockchain.getBlock(top.getHash());
        assert block != null;

        BlockUndo undo = utxoProcessor.processBlockConnected(block);

        // Store undo data
        blockchain.storeBlockUndo(top, undo);

        // Update transaction pool
        transactionProcessor.processTopBlockConnected(block);

        // Set the new top in the main chain
        blockchain.pushTopBlock(top);
    }

}
