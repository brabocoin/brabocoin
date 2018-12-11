package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.block.BlockRule;
import org.brabocoin.brabocoin.validation.fact.CompositeRuleFailMarker;
import org.brabocoin.brabocoin.validation.rule.RuleBookFailMarker;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidationResult;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;

@ValidationRule(name="Contextual transaction validation")
public class ContextualTransactionCheckBlkRule extends BlockRule {

    private TransactionValidator transactionValidator;

    @CompositeRuleFailMarker
    private RuleBookFailMarker childFailMarker;

    @Override
    public boolean isValid() {
        for (Transaction transaction : block.getTransactions()) {
            if (transaction.isCoinbase()) {
                continue;
            }
            TransactionValidationResult result =
                transactionValidator.checkTransactionBlockContextual(transaction);

            if (!result.isPassed()) {
                childFailMarker = result.getFailMarker();
                return false;
            }
        }

        return true;
    }
}
