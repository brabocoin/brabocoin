package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.Consensus;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

/**
 * Transaction rule
 */
@Rule(name = "Input value rule", description = "The amount of the referenced output of the input must be positive and the sum of these amounts must be smaller than the max transaction range decided by consensus.")
public class InputValueTxRange {
    @Condition
    public boolean valid(@Fact("transaction") Transaction transaction, @Fact("transactionProcessor") TransactionProcessor transactionProcessor, @Fact("consensus") Consensus consensus) {
        long sum = 0L;
        for (Input input : transaction.getInputs()) {
            UnspentOutputInfo unspentOutputInfo;
            try {
                unspentOutputInfo = transactionProcessor.findUnspentOutputInfo(input);
            } catch (DatabaseException e) {
                return false;
            }

            if (unspentOutputInfo == null) {
                return false;
            }

            if (unspentOutputInfo.getAmount() <= 0) {
                return false;
            }

            sum += unspentOutputInfo.getAmount();
            if (sum > consensus.getMaxTransactionRange()) {
                return false;
            }
        }

        return true;
    }
}
