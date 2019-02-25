package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 * <p>
 * Reject if the transaction is a coinbase transaction.
 */
@ValidationRule(name="Transaction is not coinbase", failedName = "Transaction is coinbase", description = "The transaction is not a coinbase transaction.")
public class CoinbaseCreationTxRule extends TransactionRule {

    @DescriptionField
    private boolean isCoinbase;

    public boolean isValid() {
        isCoinbase = transaction.isCoinbase();
        return !isCoinbase;
    }
}
