package org.brabocoin.brabocoin.validation.consensus;

import org.brabocoin.brabocoin.model.Hash;

/**
 * Consensus rules.
 */
public class Consensus extends MutableConsensus {

    private final int maxBlockSize;
    private final int maxNonceSize;
    private final int coinbaseMaturityDepth;
    private final int blockReward;
    private final int minimumTransactionFee;
    private final Hash cachedTargetValue;

    public Consensus() {
        this(new MutableConsensus());
    }

    public Consensus(MutableConsensus consensus) {
        this.maxBlockSize = consensus.getMaxBlockSize();
        this.maxNonceSize = consensus.getMaxNonceSize();
        this.coinbaseMaturityDepth = consensus.getCoinbaseMaturityDepth();
        this.blockReward = consensus.getBlockReward();
        this.minimumTransactionFee = consensus.getMinimumTransactionFee();
        this.cachedTargetValue = consensus.getTargetValue();
    }

    @Override
    public int getMaxBlockSize() {
        return maxBlockSize;
    }

    @Override
    public int getMaxNonceSize() {
        return maxNonceSize;
    }

    @Override
    public int getCoinbaseMaturityDepth() {
        return coinbaseMaturityDepth;
    }

    @Override
    public int getBlockReward() {
        return blockReward;
    }

    @Override
    public int getMinimumTransactionFee() {
        return minimumTransactionFee;
    }

    @Override
    public Hash getTargetValue() {
        return cachedTargetValue;
    }
}
