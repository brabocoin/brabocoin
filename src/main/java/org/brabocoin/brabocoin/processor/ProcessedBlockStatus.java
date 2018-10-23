package org.brabocoin.brabocoin.processor;

/**
 * The result of processing a new block.
 */
public enum ProcessedBlockStatus {

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
