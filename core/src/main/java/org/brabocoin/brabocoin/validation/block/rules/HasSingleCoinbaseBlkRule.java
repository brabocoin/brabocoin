package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Has single coinbase transaction", failedName = "Block has multiple coinbase transactions", description = "The block has no other coinbase transactions, besides the first transaction.")
public class HasSingleCoinbaseBlkRule extends BlockRule {

    @DescriptionField
    private boolean hasSingleCoinbase;

    @Override
    public boolean isValid() {
        hasSingleCoinbase = block.getTransactions().stream()
            .skip(1)
            .noneMatch(Transaction::isCoinbase);
        return hasSingleCoinbase;
    }
}
