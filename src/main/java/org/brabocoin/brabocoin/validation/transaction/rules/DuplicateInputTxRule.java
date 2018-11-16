package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

import java.util.HashSet;
import java.util.List;

/**
 * Transaction rule
 *
 * Transactions can not contain duplicate inputs.
 */
public class DuplicateInputTxRule extends TransactionRule {
    public boolean isValid() {
        // Check for no duplicates
        List<Input> inputs = transaction.getInputs();
        return inputs.size() == new HashSet<>(inputs).size();
    }
}
