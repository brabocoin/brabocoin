package org.brabocoin.brabocoin.validation;

public class RuleBookResult {
    private boolean passed;
    private Class failedRule;

    public RuleBookResult(boolean passed, Class failedRule) {
        this.passed = passed;
        this.failedRule = failedRule;
    }

    public RuleBookResult(boolean passed) {
        this.passed = passed;
    }

    public boolean isPassed() {
        return passed;
    }

    public Class getFailedRule() {
        return failedRule;
    }
}
