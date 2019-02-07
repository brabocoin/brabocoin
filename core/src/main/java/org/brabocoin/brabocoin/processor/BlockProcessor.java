package org.brabocoin.brabocoin.processor;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.dal.BlockInfo;
import org.brabocoin.brabocoin.model.dal.BlockUndo;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.ValidationStatus;
import org.brabocoin.brabocoin.validation.block.BlockValidationResult;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.brabocoin.brabocoin.util.ByteUtil.toHexString;

/**
 * Processes a new incoming block.
 */
public class BlockProcessor {

    private static final Logger LOGGER = Logger.getLogger(BlockProcessor.class.getName());

    private final @NotNull Set<BlockProcessorListener> listeners;

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
        this.listeners = new HashSet<>();
    }

    /**
     * Synchronize the main chain to be consistent with the chain UTXO set.
     * <p>
     * Used when initializing the blockchain from disk. The main chain needs to be loaded in
     * memory according to the last processed block in the UTXO set.
     *
     * @throws DatabaseException
     *     When either of the databases is not available.
     * @throws IllegalStateException
     *     When the blockchain could not be synced with the UTXO set. Most likely either one of
     *     the databases is corrupt, in which case the node has to rebuild all indices.
     */
    public synchronized void syncMainChainWithUTXOSet() throws DatabaseException,
                                                               IllegalStateException {
        LOGGER.info("Syncing main chain with UTXO set.");

        listeners.forEach(BlockProcessorListener::onSyncWithUTXOSetStarted);

        // Get the top block from the UTXO set
        IndexedBlock block = blockchain.getIndexedBlock(utxoProcessor.getLastProcessedBlockHash());

        if (block == null) {
            LOGGER.severe("Main chain could not be synced: requested top block is not stored.");
            throw new IllegalStateException(
                "Main chain could not be synced: requested top block is not stored.");
        }

        Deque<IndexedBlock> fork = findValidFork(block);

        if (fork == null) {
            LOGGER.severe(
                "Main chain could not be synced: no fork from the top block to the main chain is "
                    + "found.");
            throw new IllegalStateException(
                "Main chain could not be synced: no fork from the top block to the main chain is "
                    + "found.");
        }

        IndexedBlock onChain = fork.pop();

        if (!blockchain.getMainChain().getTopBlock().getHash().equals(onChain.getHash())) {
            LOGGER.severe(
                "Main chain could not be synced: requested top block forks before current top "
                    + "block. UTXO set is corrupted.");
            throw new IllegalStateException(
                "Main chain could not be synced: requested top block forks before current top "
                    + "block. UTXO set is corrupted.");
        }

        // Connect all the blocks
        while (!fork.isEmpty()) {
            blockchain.pushTopBlock(fork.pop());
        }

        listeners.forEach(BlockProcessorListener::onSyncWithUTXOSetFinished);

        LOGGER.info(() -> MessageFormat.format(
            "Synced main chain to height={0}.",
            blockchain.getMainChain().getHeight()
        ));
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
     * @param minedByMe
     *     Whether this user mined this block.
     * @return The status of the block that is added.
     * @throws DatabaseException
     *     When the block database is not available.
     */
    public synchronized ValidationStatus processNewBlock(@NotNull Block block, boolean minedByMe)
        throws DatabaseException {
        LOGGER.fine("Processing new block.");

        // Check if the block is valid
        BlockValidationResult result = blockValidator.validate(
            block,
            BlockValidator.INCOMING_BLOCK
        );
        ValidationStatus status = result.getStatus();

        if (status == ValidationStatus.INVALID) {
            // Add to the recent rejects
            blockchain.addRejected(block, result);

            LOGGER.log(
                Level.INFO,
                MessageFormat.format("New block is invalid, rulebook result: {0}", result)
            );
            return ValidationStatus.INVALID;
        }

        if (status == ValidationStatus.ORPHAN) {
            LOGGER.info("New block is added as orphan.");
            blockchain.addOrphan(block);
            return ValidationStatus.ORPHAN;
        }

        // Store the block on disk
        IndexedBlock indexedBlock = storeBlock(block, minedByMe);

        // Check if any orphan blocks are descendants and can be added as well
        // Return the leaf blocks of the new family, which are new top candidates
        Set<IndexedBlock> topCandidates = processOrphansTopCandidates(indexedBlock);

        // Update the main chain if necessary
        updateMainChain(topCandidates);

        LOGGER.info("New block is added to the blockchain.");

        listeners.forEach(l -> l.onValidBlockProcessed(block));

        return ValidationStatus.VALID;
    }

    /**
     * Add a {@link BlockProcessorListener} to the listener set.
     *
     * @param listener
     *     The listener to add.
     */
    public void addBlockProcessorListener(BlockProcessorListener listener) {
        listeners.add(listener);
    }

    private synchronized IndexedBlock storeBlock(
        @NotNull Block block, boolean minedByMe) throws DatabaseException {
        BlockInfo info = blockchain.storeBlock(block, minedByMe);
        Hash hash = block.getHash();
        return new IndexedBlock(hash, info);
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
    private synchronized @NotNull Set<IndexedBlock> processOrphansTopCandidates(
        @NotNull IndexedBlock newParent) throws DatabaseException {
        LOGGER.fine("Processing orphans for top candidates.");

        Set<IndexedBlock> topCandidates = new HashSet<>();
        topCandidates.add(newParent);

        Deque<IndexedBlock> queue = new ArrayDeque<>();
        queue.add(newParent);

        while (!queue.isEmpty()) {
            IndexedBlock parent = queue.remove();

            // Find any orphan descendants and mark them as part of the chain
            Set<Block> descendants = blockchain.removeOrphansOfParent(parent.getHash());

            // For every descendant, check if it is valid now
            for (Block descendant : descendants) {
                ValidationStatus status = blockValidator.validate(
                    descendant,
                    BlockValidator.AFTER_ORPHAN
                )
                    .getStatus();

                // Re-add to orphans if not status is orphan again (should not happen)
                if (status == ValidationStatus.ORPHAN) {
                    blockchain.addOrphan(descendant);
                }

                if (status == ValidationStatus.VALID) {
                    // The orphan is now valid, store on disk
                    IndexedBlock indexedDescendant = storeBlock(descendant, false);

                    // Add to top candidates
                    topCandidates.add(indexedDescendant);

                    // Look deeper for more descendants
                    queue.add(indexedDescendant);

                    LOGGER.finest(() -> MessageFormat.format(
                        "Added orphan {0} as top candidate.",
                        toHexString(indexedDescendant.getHash().getValue())
                    ));
                }
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
    private synchronized void updateMainChain(
        Set<IndexedBlock> topCandidates) throws DatabaseException {
        LOGGER.fine("Updating main chain.");

        // Add the current top to the top candidates
        IndexedBlock currentTop = blockchain.getMainChain().getTopBlock();
        Set<IndexedBlock> allCandidates = new HashSet<>(topCandidates);
        allCandidates.add(currentTop);

        // Attempt reorganization while there are still candidates
        reorganization:
        while (true) {
            // Select the best top candidate
            IndexedBlock bestCandidate = consensus.bestValidBlock(allCandidates);

            if (bestCandidate == null) {
                // No valid candidates are found, which means the current top is no valid
                // candidate as well
                // TODO: error handling?
                LOGGER.severe("Current main chain is invalid.");
                return;
            }

            // If top does not change, do nothing
            if (currentTop.equals(bestCandidate)) {
                LOGGER.info("Main chain is not updated, current top is the best block.");
                return;
            }

            // Find the fork that needs to become active
            Deque<IndexedBlock> fork = findValidFork(bestCandidate);
            if (fork == null) {
                // No valid fork is found, remove the best candidate
                LOGGER.info(
                    "Fork is invalid, removing the selected candidate and attempt re-selecting "
                        + "new candidate.");
                allCandidates.remove(bestCandidate);
                continue;
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
                if (!connectTopBlock(newBlock)) {
                    // Block connection failed, block is invalid
                    blockchain.setBlockInvalid(newBlock.getHash());

                    // Remove this candidate from the candidate set
                    allCandidates.remove(bestCandidate);
                    continue reorganization;
                }
            }

            LOGGER.info("Main chain is updated with new top block.");
            LOGGER.finest(MessageFormat.format(
                "New top block {0}.",
                toHexString(blockchain.getMainChain().getTopBlock().getHash().getValue())
            ));
            return;
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
     * <p>
     * When a block on the fork path is invalid, {@code null} is returned as no valid fork path
     * exists.
     *
     * @param block
     *     The block to find the fork to the main chain for.
     * @return All the blocks present on the fork, in-order, up to and including the first block
     * on the main chain, or {@code null} when no valid path back to the main chain could be found.
     * @throws DatabaseException
     *     When the block database is not available.
     */
    private synchronized @Nullable Deque<IndexedBlock> findValidFork(
        @NotNull IndexedBlock block) throws DatabaseException {
        LOGGER.fine("Find a fork.");

        Deque<IndexedBlock> fork = new ArrayDeque<>();
        IndexedBlock parent = block;

        // Backtrack to the main chain, loading previous blocks from the database
        while (parent != null && !blockchain.getMainChain().contains(parent)) {
            // If parent is known to be invalid, discard this fork
            if (!parent.getBlockInfo().isValid()) {
                LOGGER.fine("Parent on fork is invalid, no valid fork found.");
                return null;
            }

            fork.push(parent);
            parent = blockchain.getIndexedBlock(parent.getBlockInfo().getPreviousBlockHash());
        }

        // If no fork is found, return null
        if (parent == null) {
            LOGGER.fine("No fork is found because parent is missing.");
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
    private synchronized void disconnectTop() throws DatabaseException {
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
     * @return {@code true} when the block is valid and added to the main chain, or {@code false}
     * when the block is invalid and thus not added to the main chain.
     * @throws DatabaseException
     *     When the blocks database is not available.
     */
    private synchronized boolean connectTopBlock(
        @NotNull IndexedBlock top) throws DatabaseException {
        LOGGER.finest(() -> MessageFormat.format(
            "Connecting block {0}",
            toHexString(top.getHash().getValue())
        ));

        // Read block from disk
        Block block = blockchain.getBlock(top.getHash());
        assert block != null;

        if (!blockValidator.validate(block, BlockValidator.CONNECT_TO_CHAIN).isPassed()) {
            return false;
        }

        BlockUndo undo = utxoProcessor.processBlockConnected(block);

        // Store undo data
        blockchain.storeBlockUndo(top, undo);

        // Update transaction pool
        transactionProcessor.processTopBlockConnected(block);

        // Set the new top in the main chain
        blockchain.pushTopBlock(top);

        return true;
    }

}
