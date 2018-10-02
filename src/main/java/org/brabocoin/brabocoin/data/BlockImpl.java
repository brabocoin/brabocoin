package org.brabocoin.brabocoin.data;

public class BlockImpl implements Block {

    /**
     * Hash of previous block.
     */
    private final Hash previousBlockHash;

    /**
     * Hash of the merkle root of this block.
     */
    private final Hash merkleRoot;

    /**
     * Target value of the proof-of-work of this block.
     */
    private final Hash targetValue;

    /**
     * Nonce used for the proof-of-work.
     */
    private final long nonce;

    /**
     * Timestamp when the block is created.
     */
    private final long timestamp;

    /**
     * Create a new block.
     * @param previousBlockHash hash of previous block
     * @param merkleRoot hash of merkle root
     * @param targetValue target value of proof-of-work
     * @param nonce nonce for proof-of-work
     * @param timestamp timestamp when block is created
     */
    public BlockImpl(Hash previousBlockHash, Hash merkleRoot, Hash targetValue,
            long nonce, long timestamp) {
        this.previousBlockHash = previousBlockHash;
        this.merkleRoot = merkleRoot;
        this.targetValue = targetValue;
        this.nonce = nonce;
        this.timestamp = timestamp;
    }

    @Override
    public Hash getPreviousBlockHash() {
        return previousBlockHash;
    }

    @Override
    public Hash getMerkleRoot() {
        return merkleRoot;
    }

    @Override
    public Hash getTargetValue() {
        return targetValue;
    }

    @Override
    public long getNonce() {
        return nonce;
    }

    @Override
    public long getTimeStamp() {
        return timestamp;
    }
}
