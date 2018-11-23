package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 * <p>
 * The referenced outputs must be present in the UTXO set.
 */
public class ValidInputUTXOTxRule extends TransactionRule {

    private ReadonlyUTXOSet utxoSet;

    public boolean isValid() {
        return transaction.getInputs()
            .stream()
            .allMatch(i -> {
                try {
                    return utxoSet.isUnspent(i);
                }
                catch (DatabaseException e) {
                    e.printStackTrace();
                    return false;
                }
            });
    }
}
