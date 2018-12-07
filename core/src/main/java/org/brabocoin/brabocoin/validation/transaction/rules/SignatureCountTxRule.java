package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

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
