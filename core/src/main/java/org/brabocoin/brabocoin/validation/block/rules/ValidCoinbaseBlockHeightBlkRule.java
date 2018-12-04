package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.block.BlockRule;

public class ValidCoinbaseBlockHeightBlkRule extends BlockRule {

    @Override
    public boolean isValid() {
        return block.getTransactions().get(0)
            .getInputs().get(0)
            .getReferencedOutputIndex() == block.getBlockHeight();
    }
}
