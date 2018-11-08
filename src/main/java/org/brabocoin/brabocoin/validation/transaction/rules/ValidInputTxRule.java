package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
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
@Rule(name = "Valid input rule", description = "The inputs used in the transaction must be valid.")
public class ValidInputTxRule {
    @Condition
    public boolean valid(@Fact("transaction") Transaction transaction, @Fact("TransactionProcessor") TransactionProcessor transactionProcessor, @Fact("consensus") Consensus consensus) {
        long outputSum = 0L;
        for (Output output : transaction.getOutputs()) {
            if (output.getAmount() <= 0) {
                return false;
            }

            outputSum += output.getAmount();
            if (outputSum > consensus.getMaxTransactionRange()) {
                return false;
            }
        }

        long inputSum = 0L;
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

            inputSum += unspentOutputInfo.getAmount();
            if (inputSum > consensus.getMaxTransactionRange()) {
                return false;
            }
        }

        return inputSum > outputSum;
    }
}
