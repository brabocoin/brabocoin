package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.block.BlockRule;

public class HasCoinbaseBlkRule extends BlockRule {
    @Override
    public boolean valid() {
        return block.getTransactions().get(0).isCoinbase();
    }
}
