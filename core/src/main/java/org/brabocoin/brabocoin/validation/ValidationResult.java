package org.brabocoin.brabocoin.validation;

import org.brabocoin.brabocoin.validation.rule.RuleBookFailMarker;
import org.brabocoin.brabocoin.validation.rule.RuleBookResult;

public abstract class ValidationResult extends RuleBookResult {

    private ValidationStatus status;

    /**
     * Constructor for a rule book result that is not passed, given the failed rule class.
     *
     * @param failedRule
     *     The rule class for which the rule book failed
     */
    protected ValidationResult(RuleBookFailMarker failedRule) {
        super(failedRule);
    }

    /**
     * Passed rulebook result constructor.
     */
    protected ValidationResult() {
        super();
    }

    /**
     * Deduces the validation status from the success flag and/or failed rule.
     *
     * @return Validation status
     */
    protected abstract ValidationStatus deductStatus();

    /**
     * Gets the status using lazy deduction.
     *
     * @return Validation status
     */
    public ValidationStatus getStatus() {
        if (status == null) {
            status = deductStatus();
        }
        return status;
    }

    @Override
    public String toString() {
        return getStatus() == ValidationStatus.VALID ? "valid" : "invalid";
    }
}
