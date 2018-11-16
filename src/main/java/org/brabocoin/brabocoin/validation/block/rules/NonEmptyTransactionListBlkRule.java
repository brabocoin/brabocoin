package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.block.BlockRule;

public class NonEmptyTransactionListBlkRule extends BlockRule {
    @Override
    public boolean isValid() {
        return block.getTransactions().size() > 0;
    }
}
