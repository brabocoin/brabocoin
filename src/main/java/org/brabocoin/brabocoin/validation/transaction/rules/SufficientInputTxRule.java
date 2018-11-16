package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionRuleUtil;

/**
 * Transaction rule
 *
 * The sum of inputs must be greater than the sum of outputs.
 */
public class SufficientInputTxRule extends TransactionRule {
    private ReadonlyUTXOSet utxoSet;

    public boolean isValid() {
        try {
            return TransactionRuleUtil.computeFee(transaction, utxoSet) > 0;
        } catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
