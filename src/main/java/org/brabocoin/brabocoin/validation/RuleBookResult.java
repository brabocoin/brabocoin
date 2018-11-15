package org.brabocoin.brabocoin.validation;

public class RuleBookResult {
    protected boolean passed;
    protected Class failedRule;

    /**
     * Creates a rule book result that is not passed, given the failed rule class.
     *
     * @param failedRule The rule class for which the rule book failed
     */
    public RuleBookResult(Class failedRule) {
        this.passed = false;
        this.failedRule = failedRule;
    }

    /**
     * Creates a passed rule book result.
     */
    public RuleBookResult() {
        this.passed = true;
    }

    public boolean isPassed() {
        return passed;
    }

    public Class getFailedRule() {
        return failedRule;
    }
}
