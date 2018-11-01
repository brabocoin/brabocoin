package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.chain.IndexedChain;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.model.Transaction;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

/**
 * Transaction rule
 */
@Rule(name = "Duplicate transactions rule", description = "Transactions that exist in the transaction pool or in the main chain are invalid.")
public class DuplicateTxRule {
    @Condition
    public boolean valid(@Fact("transaction") Transaction transaction, @Fact("pool") TransactionPool pool, @Fact("chain") IndexedChain mainChain) {
        // TODO: Rule 8 of the bitcoin protocol rules, how are we supposed to do this?
        return true;
    }
}
