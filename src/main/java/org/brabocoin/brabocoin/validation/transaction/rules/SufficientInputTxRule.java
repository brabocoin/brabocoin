package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 *
 * The sum of inputs must be greater than the sum of outputs.
 */
public class SufficientInputTxRule extends TransactionRule {
    private TransactionProcessor transactionProcessor;

    public boolean valid() {
        try {
            return transactionProcessor.computeFee(transaction) > 0;
        } catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
