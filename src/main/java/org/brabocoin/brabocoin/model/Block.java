package org.brabocoin.brabocoin.model;

/**
 * A block in the blockchain.
 */
public interface Block {

    /**
     * Get the hash of the previous block in the blockchain.
     * @return Hash of previous block
     */
    Hash getPreviousBlockHash();

    /**
     * Get the hash of the merkle root of this block.
     * @return Hash of merkle root
     */
    Hash getMerkleRoot();

    /**
     * Get the target value used for the proof-of-work in this block.
     * @return The target value
     */
    Hash getTargetValue();

    /**
     * Get the nonce used for the proof-or-work.
     * @return The nonce
     */
    long getNonce();

    /**
     * Get the timestamp at which the block was created
     * in milliseconds counting from January 1st, 1970.
     * @return The timestamp
     */
    long getTimeStamp();
}
