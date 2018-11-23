package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.validation.block.BlockRule;

public class UniqueUnspentCoinbaseBlkRule extends BlockRule {

    private ReadonlyUTXOSet utxoSet;

    @Override
    public boolean isValid() {
        try {
            return !utxoSet.isUnspent(block.getCoinbaseTransaction().getHash(), 0);
        }
        catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
