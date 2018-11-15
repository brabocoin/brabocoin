package org.brabocoin.brabocoin.validation;

public abstract class ValidationResult extends RuleBookResult {
    private ValidationStatus status;

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
}
