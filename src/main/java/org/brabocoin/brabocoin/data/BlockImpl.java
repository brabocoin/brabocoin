package org.brabocoin.brabocoin.data;

public class BlockImpl implements Block {

    private Hash previousBlockHash;

    private Hash merkleRoot;

    private Hash targetValue;

    private long nonce;

    private long timestamp;

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
