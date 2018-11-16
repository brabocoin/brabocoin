package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 *
 * Inputs can only be empty on a coinbase transaction, empty output is never allowed.
 */
public class InputOutputNotEmptyTxRule extends TransactionRule {
    public boolean isValid() {
        if (transaction.getOutputs().isEmpty()) {
            return false;
        }

        // If coinbase, inputs can be empty
        if (transaction.isCoinbase()) {
            return !transaction.getInputs().isEmpty();
        }

        return true;
    }
}
