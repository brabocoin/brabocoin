package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="Non-empty transaction list", failedName = "Empty transaction list", description = "The block contains at least one transaction.")
public class NonEmptyTransactionListBlkRule extends BlockRule {

    @DescriptionField
    private int transactionsSize;

    @Override
    public boolean isValid() {
        transactionsSize = block.getTransactions().size();
        return transactionsSize > 0;
    }
}
