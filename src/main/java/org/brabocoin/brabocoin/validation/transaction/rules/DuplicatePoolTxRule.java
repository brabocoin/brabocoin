package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 *
 * Transactions that exist in the transaction pool are invalid.
 */
public class DuplicatePoolTxRule extends TransactionRule {
    private TransactionPool transactionPool;

    public boolean valid() {
        return !transactionPool.contains(transaction.getHash());
    }
}
