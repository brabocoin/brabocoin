package org.brabocoin.brabocoin.validation.transaction;

import org.brabocoin.brabocoin.validation.RuleBookResult;
import org.brabocoin.brabocoin.validation.ValidationResult;
import org.brabocoin.brabocoin.validation.ValidationStatus;
import org.brabocoin.brabocoin.validation.transaction.rules.ValidInputTxRule;

public class TransactionValidationResult extends ValidationResult {
    public TransactionValidationResult(RuleBookResult result) {
        this.passed = result.isPassed();
        this.failedRule = result.getFailedRule();
    }

    @Override
    protected ValidationStatus deductStatus() {
        if (isPassed()) {
            return ValidationStatus.VALID;
        }

        assert getFailedRule() != null;

        if (getFailedRule() == ValidInputTxRule.class) {
            return ValidationStatus.ORPHAN;
        }

        return ValidationStatus.INVALID;
    }
}
