package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.Consensus;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

/**
 * Transaction rule
 */
@Rule(name = "Output value rule", description = "Output values must be positive and the sum must be smaller than the max transaction range decided by consensus.")
public class OutputValueTxRule {
    @Condition
    public boolean valid(@Fact("transaction") Transaction transaction, @Fact("consensus") Consensus consensus) {
        long sum = 0L;
        for (Output output : transaction.getOutputs()) {
            if (output.getAmount() <= 0) {
                return false;
            }

            sum += output.getAmount();
            if (sum > consensus.getMaxTransactionRange()) {
                return false;
            }
        }

        return true;
    }
}
