package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

@ValidationRule(name="Correct signature amount", failedName = "Incorrect amount of signatures", description = "The amount of signatures is equal to the amount of inputs.")
public class SignatureCountTxRule extends TransactionRule {

    @Override
    public boolean isValid() {
        int signatureCount = transaction.getSignatures().size();
        if (transaction.isCoinbase()) {
            return signatureCount == 0;
        }

        return transaction.getInputs().size() == signatureCount;
    }
}
