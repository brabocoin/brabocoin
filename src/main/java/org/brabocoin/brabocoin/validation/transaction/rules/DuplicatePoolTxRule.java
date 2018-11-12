package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 *
 * Transactions that exist in the transaction pool are invalid.
 */
public class DuplicatePoolTxRule extends TransactionRule {
    private TransactionPool pool;

    public boolean valid() {
        return !pool.contains(transaction.computeHash());
    }
}
