package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 * <p>
 * Reject if the transaction is a coinbase transaction.
 */
public class CoinbaseCreationTxRule extends TransactionRule {

    public boolean isValid() {
        return !transaction.isCoinbase();
    }
}
