package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 * <p>
 * Inputs can only be empty on a coinbase transaction, empty output is never allowed.
 */
@ValidationRule(name="Non-empty input and output lists", failedName = "Empty input or output list", description = "The transaction has at least one input and one output.")
public class InputOutputNotEmptyTxRule extends TransactionRule {

    @DescriptionField
    private int inputCount;
    @DescriptionField
    private int outputCount;
    @DescriptionField
    private boolean isCoinbase;

    public boolean isValid() {
        isCoinbase = transaction.isCoinbase();

        inputCount = transaction.getInputs().size();
        outputCount = transaction.getOutputs().size();

        return inputCount > 0 && outputCount > 0;
    }
}
