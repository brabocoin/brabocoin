package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Unique coinbase transaction", description = "The coinbase transaction of the block is unique and not already present as unspent transaction output.")
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
