package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Has single coinbase transaction", description = "The block has no other coinbase transactions, besides the first transaction.")
public class HasSingleCoinbaseBlkRule extends BlockRule {

    @Override
    public boolean isValid() {
        return block.getTransactions().stream()
            .skip(1)
            .noneMatch(Transaction::isCoinbase);
    }
}
