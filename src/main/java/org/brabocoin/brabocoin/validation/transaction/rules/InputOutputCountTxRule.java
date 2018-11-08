package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.model.Transaction;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

/**
 * Transaction rule
 */
@Rule(name = "Input and output count rule", description = "Inputs can only be empty on a coinbase transaction, empty output is never allowed.")
public class InputOutputCountTxRule {
    @Condition
    public boolean valid(@Fact("transaction") Transaction transaction) {
        if (transaction.isCoinbase()) {
            // If coinbase, inputs can be empty, only check outputs
            return transaction.getOutputs().size() > 0;
        } else {
            // else, inputs can not be empty
            return transaction.getInputs().size() > 0 && transaction.getOutputs().size() > 0;
        }
    }
}
