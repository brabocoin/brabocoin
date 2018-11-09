package org.brabocoin.brabocoin.validation.transaction.rules;

import com.deliveredtechnologies.rulebook.annotation.Given;
import com.deliveredtechnologies.rulebook.annotation.Rule;
import com.deliveredtechnologies.rulebook.annotation.When;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 *
 * Transactions that exist in the transaction pool are invalid.
 */
@Rule(name = "Duplicate transactions rule")
public class DuplicatePoolTxRule extends TransactionRule {
    @Given("pool")
    private TransactionPool pool;

    @When
    public boolean valid() {
        return !pool.contains(transaction.computeHash());
    }
}
