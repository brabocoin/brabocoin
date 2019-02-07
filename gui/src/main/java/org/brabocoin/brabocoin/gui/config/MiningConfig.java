package org.brabocoin.brabocoin.gui.config;

import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.crypto.PublicKey;

public class MiningConfig {
    private PublicKey rewardPublicKey;
    private IndexedBlock parentBlock;

    public MiningConfig(PublicKey rewardPublicKey) {
        this(rewardPublicKey, null);
    }

    public MiningConfig(PublicKey rewardPublicKey, IndexedBlock parentBlock) {
        this.rewardPublicKey = rewardPublicKey;
        this.parentBlock = parentBlock;
    }

    public PublicKey getRewardPublicKey() {
        return rewardPublicKey;
    }

    public void setRewardPublicKey(PublicKey rewardPublicKey) {
        this.rewardPublicKey = rewardPublicKey;
    }

    public IndexedBlock getParentBlock() {
        return parentBlock;
    }

    public void setParentBlock(IndexedBlock parentBlock) {
        this.parentBlock = parentBlock;
    }

    public boolean hasCustomParentBlock() {
        return parentBlock != null;
    }
}
