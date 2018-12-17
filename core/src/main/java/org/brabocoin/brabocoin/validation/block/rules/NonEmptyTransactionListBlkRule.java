package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;

@ValidationRule(name="non-empty transaction list", description = "The block contains at least one transaction.")
public class NonEmptyTransactionListBlkRule extends BlockRule {

    @Override
    public boolean isValid() {
        return block.getTransactions().size() > 0;
    }
}
