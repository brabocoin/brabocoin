package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionUtil;

/**
 * Transaction rule
 * <p>
 * The sum of inputs must be greater than the sum of outputs.
 */
@ValidationRule(name="Sufficient transaction fee", failedName = "Insufficient transaction fee", description = "The transaction fee is larger than the minimum transaction fee defined in consensus.")
public class SufficientInputTxRule extends TransactionRule {

    private ReadonlyUTXOSet utxoSet;

    public boolean isValid() {
        try {
            return TransactionUtil.computeFee(
                transaction,
                utxoSet
            ) >= consensus.getMinimumTransactionFee();
        }
        catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
