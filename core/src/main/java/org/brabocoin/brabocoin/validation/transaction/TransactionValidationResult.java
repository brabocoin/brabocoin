package org.brabocoin.brabocoin.validation.transaction;

import org.brabocoin.brabocoin.validation.ValidationResult;
import org.brabocoin.brabocoin.validation.ValidationStatus;
import org.brabocoin.brabocoin.validation.rule.RuleBookFailMarker;
import org.brabocoin.brabocoin.validation.rule.RuleBookResult;
import org.brabocoin.brabocoin.validation.transaction.rules.ValidInputUTXOTxRule;

public class TransactionValidationResult extends ValidationResult {

    /**
     * Create transaction validation result from rulebook result
     *
     * @param result
     *     Rule book result
     * @return Transaction validation result
     */
    public static TransactionValidationResult from(RuleBookResult result) {
        if (result.isPassed()) {
            return passed();
        }
        return TransactionValidationResult.failed(result.getFailMarker());
    }

    /**
     * Creates a passed rule book result.
     */
    public static TransactionValidationResult failed(RuleBookFailMarker failedRule) {
        return new TransactionValidationResult(failedRule);
    }

    /**
     * Constructor for a rule book result that is not passed, given the failed rule class.
     *
     * @param failedRule
     *     The rule class for which the rule book failed
     */
    private TransactionValidationResult(RuleBookFailMarker failedRule) {
        super(failedRule);
    }

    /**
     * Creates a passed rule book result.
     */
    public static TransactionValidationResult passed() {
        return new TransactionValidationResult();
    }

    /**
     * Passed rulebook result constructor.
     */
    private TransactionValidationResult() {
        super();
    }


    @Override
    protected ValidationStatus deductStatus() {
        if (isPassed()) {
            return ValidationStatus.VALID;
        }

        assert getFailMarker() != null;

        if (getFailMarker().getFailedRule() == ValidInputUTXOTxRule.class) {
            return ValidationStatus.ORPHAN;
        }

        return ValidationStatus.INVALID;
    }
}
