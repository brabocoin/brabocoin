package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 * <p>
 * Inputs can only be empty on a coinbase transaction, empty output is never allowed.
 */
public class InputOutputNotEmptyTxRule extends TransactionRule {

    public boolean isValid() {
        if (transaction.isCoinbase()) {
            // If coinbase, inputs can be empty, only check outputs
            return transaction.getOutputs().size() > 0;
        }
        else {
            // else, inputs can not be empty
            return transaction.getInputs().size() > 0 && transaction.getOutputs().size() > 0;
        }
    }
}
