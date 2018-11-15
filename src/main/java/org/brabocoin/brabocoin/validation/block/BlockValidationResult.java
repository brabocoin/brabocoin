package org.brabocoin.brabocoin.validation.block;

import org.brabocoin.brabocoin.validation.rule.RuleBookResult;
import org.brabocoin.brabocoin.validation.ValidationResult;
import org.brabocoin.brabocoin.validation.ValidationStatus;
import org.brabocoin.brabocoin.validation.block.rules.KnownParentBlkRule;

public class BlockValidationResult extends ValidationResult {
    public BlockValidationResult(RuleBookResult result) {
        this.passed = result.isPassed();
        this.failedRule = result.getFailMarker();
    }

    @Override
    protected ValidationStatus deductStatus() {
        if (isPassed()) {
            return ValidationStatus.VALID;
        }

        assert getFailMarker() != null;

        if (getFailMarker().getFailedRule() == KnownParentBlkRule.class) {
            return ValidationStatus.ORPHAN;
        }

        return ValidationStatus.INVALID;
    }
}
