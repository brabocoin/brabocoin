package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 * <p>
 * Double spending of an output in the transaction pool rejects the transaction.
 */
@ValidationRule(name="No double spending in transaction pool", failedName = "Transaction creates double spending in the transaction pool", description = "The transaction should not spend an output that was already spent by another transaction in the pool.")
public class PoolDoubleSpendingTxRule extends TransactionRule {

    private TransactionPool transactionPool;

    @DescriptionField
    private boolean noDoubleSpending;

    public boolean isValid() {
        for (Transaction poolTransaction : transactionPool) {
            /*
             * If the output reference of any input in any transaction in the pool
             * matches any output reference in any input for the given transaction,
             * the transaction is a double spend within the transaction pool.
             */
            if (poolTransaction.getInputs()
                .stream()
                .anyMatch(i -> hasMatchingOutputReference(i, transaction))) {
                noDoubleSpending = false;
                return noDoubleSpending;
            }
        }

        noDoubleSpending = true;
        return noDoubleSpending;
    }

    /**
     * Check if the transaction has an input that matches the output reference of the given input.
     *
     * @param input
     *     The input to check
     * @param transaction
     *     The transaction to check every input for.
     * @return Whether the transaction has an input that referneces the same output as the given
     * input.
     */
    private boolean hasMatchingOutputReference(Input input, Transaction transaction) {
        return transaction.getInputs()
            .stream()
            .anyMatch(input::equals);
    }
}
