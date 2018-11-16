package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.block.BlockRule;

public class LegalTransactionFeesBlkRule extends BlockRule {
    private TransactionProcessor transactionProcessor;

    @Override
    public boolean isValid() {
        long sum = 0;
        for (Transaction transaction : block.getTransactions()) {
            if (transaction.isCoinbase()) {
                continue;
            }

            long fee;
            try {
                fee = transactionProcessor.computeFee(transaction);
            } catch (DatabaseException e) {
                e.printStackTrace();
                return false;
            }

            if (fee <= 0) {
                return false;
            }

            sum += fee;
            if (sum > consensus.getMaxMoneyValue()) {
                return false;
            }
        }

        return true;
    }
}
