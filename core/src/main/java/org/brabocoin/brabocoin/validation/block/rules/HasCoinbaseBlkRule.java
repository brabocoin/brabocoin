package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="First transaction coinbase", failedName = "First transaction is not coinbase", description = "The first transaction in the block is a coinbase transaction.")
public class HasCoinbaseBlkRule extends BlockRule {

    @DescriptionField
    private boolean isCoinbase;

    @Override
    public boolean isValid() {
        Transaction coinbase = block.getCoinbaseTransaction();
        if (coinbase == null) {
            return false;
        }

        isCoinbase = coinbase.isCoinbase();
        return isCoinbase;
    }
}
