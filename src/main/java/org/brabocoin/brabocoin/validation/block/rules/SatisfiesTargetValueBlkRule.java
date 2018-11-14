package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.block.BlockRule;

public class SatisfiesTargetValueBlkRule extends BlockRule {
    @Override
    public boolean valid() {
        return block.getHash().compareTo(consensus.getTargetValue()) <= 0;
    }
}
