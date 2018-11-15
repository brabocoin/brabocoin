package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.block.BlockRule;

public class HasCoinbaseBlkRule extends BlockRule {
    @Override
    public boolean valid() {
        Transaction coinbase = block.getCoinbaseTransaction();
        if (coinbase == null) {
            return false;
        }
        return coinbase.isCoinbase();
    }
}
