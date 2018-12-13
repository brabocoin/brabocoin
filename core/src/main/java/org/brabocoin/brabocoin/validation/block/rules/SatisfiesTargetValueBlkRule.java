package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Block satisfies target value", description = "The block hash is smaller than the block's target value.")
public class SatisfiesTargetValueBlkRule extends BlockRule {

    @Override
    public boolean isValid() {
        return block.getHash().compareTo(block.getTargetValue()) <= 0;
    }
}
