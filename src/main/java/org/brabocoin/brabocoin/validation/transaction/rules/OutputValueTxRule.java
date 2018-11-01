package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

/**
 * Transaction rule
 */
@Rule(name = "Output value rule", description = "Output values must be positive")
public class OutputValueTxRule {
    @Condition
    public boolean valid(@Fact("transaction") Transaction transaction) {
        for (Output output : transaction.getOutputs()) {
            if (output.getAmount() <= 0) {
                return false;
            }
        }

        return true;
    }
}
