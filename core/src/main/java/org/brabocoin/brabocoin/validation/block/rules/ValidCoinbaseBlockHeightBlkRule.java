package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Valid coinbase block height", description = "The block height defined in the coinbase transaction is equal to the block height defined in the block header.")
public class ValidCoinbaseBlockHeightBlkRule extends BlockRule {

    @DescriptionField
    private int coinbaseBlockHeight;
    @DescriptionField
    private int blockHeight;

    @Override
    public boolean isValid() {
        coinbaseBlockHeight = block.getTransactions().get(0)
            .getInputs().get(0)
            .getReferencedOutputIndex();
        blockHeight = block.getBlockHeight();

        return coinbaseBlockHeight == blockHeight;
    }
}
