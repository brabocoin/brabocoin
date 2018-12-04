package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 * <p>
 * Inputs can only be empty on a coinbase transaction, empty output is never allowed.
 */
public class InputOutputNotEmptyTxRule extends TransactionRule {

    public boolean isValid() {
        return transaction.getInputs().size() > 0 && transaction.getOutputs().size() > 0;
    }
}
