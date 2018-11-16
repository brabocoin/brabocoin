package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.block.BlockRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;

public class ContextualTransactionCheckBlkRule extends BlockRule {
    private TransactionValidator transactionValidator;

    @Override
    public boolean isValid() {
        return block.getTransactions().stream()
                .skip(1)
                .allMatch(t -> transactionValidator.checkTransactionBlockNonContextual(t).isPassed());
    }
}
