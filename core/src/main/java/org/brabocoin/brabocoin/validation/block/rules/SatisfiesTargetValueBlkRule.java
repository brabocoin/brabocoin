package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Block satisfies target value", failedName = "Block does not satisfy target value", description = "The block hash is smaller than the block's target value.")
public class SatisfiesTargetValueBlkRule extends BlockRule {

    @DescriptionField
    private Hash blockHash;
    @DescriptionField
    private Hash targetValue;

    @Override
    public boolean isValid() {
        blockHash = block.getHash();
        targetValue = block.getTargetValue();

        return blockHash.compareTo(targetValue) <= 0;
    }
}
