package org.brabocoin.brabocoin.validation.transaction.rules;

import com.deliveredtechnologies.rulebook.annotation.Given;
import com.deliveredtechnologies.rulebook.annotation.Rule;
import com.deliveredtechnologies.rulebook.annotation.When;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

import java.util.Iterator;

/**
 * Transaction rule
 *
 * Double spending of an output in the transaction pool rejects the transaction.
 */
@Rule(name = "Double spending in transaction pool rule")
public class PoolDoubleSpendingTxRule extends TransactionRule {
    @Given("pool")
    private TransactionPool pool;

    @When
    public boolean valid() {
        for (Iterator<Transaction> it = pool.getIterator(); it.hasNext(); ) {
            Transaction poolTransaction = it.next();

            /*
             * If the output reference of any input in any transaction in the pool
             * matches any output reference in any input for the given transaction,
             * the transaction is a double spend within the transaction pool.
             */
            if (poolTransaction.getInputs()
                    .stream()
                    .anyMatch(i -> hasMatchingOutputReference(i, transaction))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if the transaction has an input that matches the output reference of the given input.
     *
     * @param input       The input to check
     * @param transaction The transaction to check every input for.
     * @return Whether the transaction has an input that referneces the same output as the given input.
     */
    private boolean hasMatchingOutputReference(Input input, Transaction transaction) {
        return transaction.getInputs()
                .stream()
                .anyMatch(input::equals);
    }
}
