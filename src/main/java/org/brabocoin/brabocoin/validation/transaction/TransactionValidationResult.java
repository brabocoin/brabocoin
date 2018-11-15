package org.brabocoin.brabocoin.validation.transaction;

import org.brabocoin.brabocoin.validation.rule.RuleBookResult;
import org.brabocoin.brabocoin.validation.ValidationResult;
import org.brabocoin.brabocoin.validation.ValidationStatus;
import org.brabocoin.brabocoin.validation.transaction.rules.ValidInputTxRule;

public class TransactionValidationResult extends ValidationResult {
    public TransactionValidationResult(RuleBookResult result) {
        this.passed = result.isPassed();
        this.failedRule = result.getFailMarker();
    }

    @Override
    protected ValidationStatus deductStatus() {
        if (isPassed()) {
            return ValidationStatus.VALID;
        }

        assert getFailMarker() != null;

        if (getFailMarker().getFailedRule() == ValidInputTxRule.class) {
            return ValidationStatus.ORPHAN;
        }

        return ValidationStatus.INVALID;
    }
}
