package org.brabocoin.brabocoin.validation.block;

import org.brabocoin.brabocoin.validation.ValidationResult;
import org.brabocoin.brabocoin.validation.ValidationStatus;
import org.brabocoin.brabocoin.validation.block.rules.KnownParentBlkRule;
import org.brabocoin.brabocoin.validation.rule.RuleBookFailMarker;
import org.brabocoin.brabocoin.validation.rule.RuleBookResult;

public class BlockValidationResult extends ValidationResult {
    /**
     * Create block validation result from rulebook result
     *
     * @param result Rule book result
     * @return Block validation result
     */
    public static BlockValidationResult from(RuleBookResult result) {
        if (result.isPassed()) {
            return passed();
        }
        return BlockValidationResult.failed(result.getFailMarker());
    }

    /**
     * Creates a passed rule book result.
     */
    public static BlockValidationResult failed(RuleBookFailMarker failedRule) {
        return new BlockValidationResult(failedRule);
    }

    /**
     * Constructor for a rule book result that is not passed, given the failed rule class.
     *
     * @param failedRule The rule class for which the rule book failed
     */
    private BlockValidationResult(RuleBookFailMarker failedRule) {
        super(failedRule);
    }

    /**
     * Creates a passed rule book result.
     */
    public static BlockValidationResult passed() {
        return new BlockValidationResult();
    }

    /**
     * Passed rulebook result constructor.
     */
    private BlockValidationResult() {
        super();
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
