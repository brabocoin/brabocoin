package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Valid coinbase block height", description = "The block height defined in the coinbase transaction is equal to the block height defined in the block header.")
public class ValidCoinbaseBlockHeightBlkRule extends BlockRule {

    @Override
    public boolean isValid() {
        return block.getTransactions().get(0)
            .getInputs().get(0)
            .getReferencedOutputIndex() == block.getBlockHeight();
    }
}
