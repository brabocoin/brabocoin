package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.model.Transaction;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import java.util.HashSet;

/**
 * Transaction rule
 */
@Rule(name = "Duplicate input rule", description = "Transactions can not contain duplicate inputs.")
public class DuplicateInputTxRule {
    @Condition
    public boolean valid(@Fact("transaction") Transaction transaction) {
        // Check for no duplicates
        return transaction.getInputs().stream().allMatch(new HashSet<>()::add);
    }
}
