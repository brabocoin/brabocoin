package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.block.BlockRule;

public class HasSingleCoinbaseBlkRule extends BlockRule {

    @Override
    public boolean isValid() {
        return block.getTransactions().stream()
            .skip(1)
            .noneMatch(Transaction::isCoinbase);
    }
}
