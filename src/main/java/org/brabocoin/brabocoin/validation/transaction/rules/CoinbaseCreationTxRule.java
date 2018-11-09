package org.brabocoin.brabocoin.validation.transaction.rules;

import com.deliveredtechnologies.rulebook.annotation.Rule;
import com.deliveredtechnologies.rulebook.annotation.When;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 *
 * Reject if the transaction is a coinbase transaction.
 */
@Rule(name = "Coinbase creation rule")
public class CoinbaseCreationTxRule extends TransactionRule {
    @When
    public boolean valid() {
        return !transaction.isCoinbase();
    }
}
