package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

import java.util.HashSet;
import java.util.Set;

/**
 * Transaction rule
 * <p>
 * Transactions can not contain duplicate inputs.
 */
public class DuplicateInputTxRule extends TransactionRule {

    public boolean isValid() {
        // Check for no duplicates
        Set<Input> seen = new HashSet<>();
        return transaction.getInputs().stream().allMatch(seen::add);
    }
}
