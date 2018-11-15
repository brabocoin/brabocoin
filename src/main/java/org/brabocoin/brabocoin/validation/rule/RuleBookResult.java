package org.brabocoin.brabocoin.validation.rule;

public class RuleBookResult {
    protected boolean passed;
    protected RuleBookFailMarker failedRule;

    /**
     * Creates a rule book result that is not passed, given the failed rule class.
     *
     * @param failedRule The rule class for which the rule book failed
     */
    public RuleBookResult(RuleBookFailMarker failedRule) {
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

    public RuleBookFailMarker getFailMarker() {
        return failedRule;
    }
}
