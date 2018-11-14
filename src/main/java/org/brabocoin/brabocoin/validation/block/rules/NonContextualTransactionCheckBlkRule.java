package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.validation.block.BlockRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;

public class NonContextualTransactionCheckBlkRule extends BlockRule {
    private TransactionValidator transactionValidator;

    @Override
    public boolean valid() {
        return block.getTransactions().stream()
                .allMatch(t -> transactionValidator.checkTransactionValid(
                        TransactionValidator.RuleLists.BLOCK_NONCONTEXTUAL,
                        t,
                        consensus,
                        // Note: these facts are not used in the non-contextual transaction rules
                        null, null, null, null, null
                ).isPassed());
    }
}
