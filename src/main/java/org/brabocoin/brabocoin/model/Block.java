package org.brabocoin.brabocoin.model;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;

@ProtoClass(BrabocoinProtos.Block.class)
public class Block {

    /**
     * Hash of previous block.
     */
    @ProtoField
    private final Hash previousBlockHash;

    /**
     * Hash of the merkle root of this block.
     */
    @ProtoField
    private final Hash merkleRoot;

    /**
     * Target value of the proof-of-work of this block.
     */
    @ProtoField
    private final Hash targetValue;

    /**
     * Nonce used for the proof-of-work.
     */
    @ProtoField
    private final ByteString nonce;

    /**
     * Timestamp when the block is created.
     */
    @ProtoField
    private final long timestamp;

    /**
     * The height of the current block.
     */
    @ProtoField
    private final long blockHeight;

    /**
     * Create a new block.
     * @param previousBlockHash hash of previous block
     * @param merkleRoot hash of merkle root
     * @param targetValue target value of proof-of-work
     * @param nonce nonce for proof-of-work
     * @param timestamp timestamp when block is created
     * @param blockHeight
     */
    public Block(Hash previousBlockHash, Hash merkleRoot, Hash targetValue,
                 ByteString nonce, long timestamp, long blockHeight) {
        this.previousBlockHash = previousBlockHash;
        this.merkleRoot = merkleRoot;
        this.targetValue = targetValue;
        this.nonce = nonce;
        this.timestamp = timestamp;
        this.blockHeight = blockHeight;
    }

    public Hash getPreviousBlockHash() {
        return previousBlockHash;
    }

    public Hash getMerkleRoot() {
        return merkleRoot;
    }

    public Hash getTargetValue() {
        return targetValue;
    }

    public ByteString getNonce() {
        return nonce;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @ProtoClass(BrabocoinProtos.Block.class)
    public static class Builder {
        @ProtoField
        private Hash.Builder previousBlockHash;
        @ProtoField
        private Hash.Builder merkleRoot;
        @ProtoField
        private Hash.Builder targetValue;
        @ProtoField
        private ByteString nonce;
        @ProtoField
        private long timestamp;
        @ProtoField
        private long blockHeight;

        public Builder setPreviousBlockHash(Hash.Builder previousBlockHash) {
            this.previousBlockHash = previousBlockHash;
            return this;
        }

        public Builder setMerkleRoot(Hash.Builder merkleRoot) {
            this.merkleRoot = merkleRoot;
            return this;
        }

        public Builder setTargetValue(Hash.Builder targetValue) {
            this.targetValue = targetValue;
            return this;
        }

        public Builder setNonce(ByteString nonce) {
            this.nonce = nonce;
            return this;
        }

        public Builder setTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public void setBlockHeight(long blockHeight) {
            this.blockHeight = blockHeight;
        }

        public Block createBlock() {
            return new Block(previousBlockHash.createHash(), merkleRoot.createHash(), targetValue.createHash(), nonce, timestamp, blockHeight);
        }
    }
}
