package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

@ValidationRule(name="Correct signature amount", failedName = "Incorrect amount of signatures", description = "The amount of signatures is equal to the amount of inputs.")
public class SignatureCountTxRule extends TransactionRule {
    @DescriptionField
    private int signatureCount;

    @DescriptionField
    private int inputCount;

    @DescriptionField
    private boolean isCoinbase;

    @Override
    public boolean isValid() {
        signatureCount = transaction.getSignatures().size();
        inputCount = transaction.getInputs().size();
        isCoinbase = transaction.isCoinbase();
        if (isCoinbase) {
            return signatureCount == 0;
        }

        return inputCount == signatureCount;
    }
}
