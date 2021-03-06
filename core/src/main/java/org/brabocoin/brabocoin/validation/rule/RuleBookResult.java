package org.brabocoin.brabocoin.validation.rule;

import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;

public class RuleBookResult {

    protected final boolean passed;

    @Nullable
    protected final RuleBookFailMarker failedRule;

    Object validatedObject;

    /**
     * Creates a failed rule book result for a given failed rule.
     */
    public static RuleBookResult failed(RuleBookFailMarker failedRule) {
        return new RuleBookResult(failedRule);
    }

    /**
     * Constructor for a rule book result that is not passed, given the failed rule class.
     *
     * @param failedRule
     *     The rule class for which the rule book failed
     */
    protected RuleBookResult(@Nullable RuleBookFailMarker failedRule) {
        this.passed = false;
        this.failedRule = failedRule;
    }

    /**
     * Creates a passed rule book result.
     */
    public static RuleBookResult passed() {
        return new RuleBookResult();
    }

    /**
     * Passed rulebook result constructor.
     */
    protected RuleBookResult() {
        this.passed = true;
        this.failedRule = null;
    }

    public boolean isPassed() {
        return passed;
    }

    public RuleBookFailMarker getFailMarker() {
        return failedRule;
    }

    @Override
    public String toString() {
        return passed ? "Passed" : MessageFormat.format("Failed, marker: {0}", failedRule);
    }

    Object getValidatedObject() {
        return validatedObject;
    }

    void setValidatedObject(Object object) {
        this.validatedObject = object;
    }
}
