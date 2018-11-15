package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.fact.CompositeRuleFailMarker;
import org.brabocoin.brabocoin.validation.rule.RuleBookFailMarker;
import org.brabocoin.brabocoin.validation.block.BlockRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidationResult;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;

public class NonContextualTransactionCheckBlkRule extends BlockRule {
    private TransactionValidator transactionValidator;

    @CompositeRuleFailMarker
    private RuleBookFailMarker childFailMarker;

    @Override
    public boolean valid() {
        for (Transaction t : block.getTransactions()) {
            TransactionValidationResult result = transactionValidator.checkTransactionValid(
                    TransactionValidator.RuleLists.BLOCK_NONCONTEXTUAL,
                    t,
                    consensus,
                    // Note: these facts are not used in the non-contextual transaction rules
                    null, null, null, null, null
            );

            if (!result.isPassed()) {
                childFailMarker = result.getFailMarker();
                return false;
            }
        }

        return true;
    }
}
