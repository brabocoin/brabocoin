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
     * The new block is valid.
     */
    VALID,

    /**
     * The block is invalid and has not been stored.
     */
    INVALID
}
