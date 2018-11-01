package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.model.Transaction;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

/**
 * Transaction rule
 */
@Rule(name = "Coinbase creation rule", description = "Coinbase transactions may only be created inside a block and not independently.")
public class CoinbaseCreationTxRule {
    @Condition
    public boolean valid(@Fact("transaction") Transaction transaction, @Fact("inBlock") boolean inBlock) {
        return !transaction.isCoinbase() || inBlock;
    }
}
