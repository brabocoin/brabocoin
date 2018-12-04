package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

public class SignatureCountTxRule extends TransactionRule {

    @Override
    public boolean isValid() {
        return transaction.isCoinbase() || transaction.getInputs()
            .size() == transaction.getSignatures().size();
    }
}
