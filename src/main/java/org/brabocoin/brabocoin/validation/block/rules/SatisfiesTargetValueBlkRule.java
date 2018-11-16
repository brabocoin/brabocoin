package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.block.BlockRule;

public class SatisfiesTargetValueBlkRule extends BlockRule {
    @Override
    public boolean isValid() {
        return block.getHash().compareTo(block.getTargetValue()) <= 0;
    }
}
