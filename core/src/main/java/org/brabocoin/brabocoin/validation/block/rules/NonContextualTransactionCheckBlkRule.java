package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.annotation.CompositeRuleList;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;
import org.brabocoin.brabocoin.validation.fact.CompositeRuleFailMarker;
import org.brabocoin.brabocoin.validation.rule.RuleBookFailMarker;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidationResult;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;

@ValidationRule(name="Non contextual transaction validation", composite = true)
public class NonContextualTransactionCheckBlkRule extends BlockRule {

    private TransactionValidator transactionValidator;

    @CompositeRuleFailMarker
    private RuleBookFailMarker childFailMarker;

    @CompositeRuleList
    private RuleList compositeRuleList = TransactionValidator.BLOCK_NONCONTEXTUAL;

    @Override
    public boolean isValid() {
        for (Transaction t : block.getTransactions()) {
            TransactionValidationResult result =
                transactionValidator.checkTransactionBlockNonContextual(t);

            if (!result.isPassed()) {
                childFailMarker = result.getFailMarker();
                return false;
            }
        }

        return true;
    }
}
