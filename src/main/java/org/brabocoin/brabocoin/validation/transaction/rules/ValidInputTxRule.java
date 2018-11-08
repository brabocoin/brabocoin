package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.processor.ProcessedTransactionStatus;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

/**
 * Transaction rule
 */
@Rule(name = "Valid input rule", description = "The inputs used in the transaction must be valid.")
public class ValidInputTxRule {
    @Condition
    public boolean valid(@Fact("transaction") Transaction transaction, @Fact("TransactionProcessor") TransactionProcessor transactionProcessor) {
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
