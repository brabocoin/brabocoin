package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.model.Transaction;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

/**
 * Transaction rule
 */
@Rule(name = "Coinbase creation rule", description = "Reject if the transaction is a coinbase transaction.")
public class CoinbaseCreationTxRule {
    @Condition
    public boolean valid(@Fact("transaction") Transaction transaction) {
        return !transaction.isCoinbase();
    }
}
