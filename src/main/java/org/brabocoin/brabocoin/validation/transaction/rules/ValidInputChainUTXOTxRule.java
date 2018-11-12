package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 *
 * The referenced outputs must be present in the chain UTXO set.
 */
public class ValidInputChainUTXOTxRule extends TransactionRule {
    private ChainUTXODatabase chainUTXODatabase;

    public boolean valid() {
        return transaction.getInputs()
                .stream()
                .allMatch(i -> {
                    try {
                        return chainUTXODatabase.isUnspent(i);
                    } catch (DatabaseException e) {
                        e.printStackTrace();
                        return false;
                    }
                });
    }
}
