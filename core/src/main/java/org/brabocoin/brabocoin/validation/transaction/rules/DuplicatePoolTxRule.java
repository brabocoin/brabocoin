package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 * <p>
 * Transactions that exist in the transaction pool are invalid.
 */
@ValidationRule(name="Transaction not already stored", failedName = "Transaction was already stored", description = "The transaction should not already be stored in the transaction pool.")
public class DuplicatePoolTxRule extends TransactionRule {

    private TransactionPool transactionPool;
    @DescriptionField
    private boolean duplicatePool;

    public boolean isValid() {
        duplicatePool = transactionPool.contains(transaction.getHash());
        return !duplicatePool;
    }
}
