package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.validation.block.BlockRule;

public class DuplicateCoinbaseBlkRule extends BlockRule {
    private ChainUTXODatabase chainUTXODatabase;

    @Override
    public boolean valid() {
        try {
            return !chainUTXODatabase.isUnspent(block.getCoinbaseTransaction().getHash(), 0);
        } catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
