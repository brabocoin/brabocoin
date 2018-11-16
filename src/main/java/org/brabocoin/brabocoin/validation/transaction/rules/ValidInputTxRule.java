package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.processor.ProcessedTransactionStatus;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 *
 * The inputs used in the transaction must be valid.
 */
public class ValidInputTxRule extends TransactionRule {
    private TransactionProcessor transactionProcessor;

    public boolean isValid() {
        try {
            if (transactionProcessor.checkInputs(transaction) == ProcessedTransactionStatus.ORPHAN) {
                return false;
            }
        } catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
