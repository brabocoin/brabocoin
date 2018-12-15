package org.brabocoin.brabocoin.validation.rule;

public interface RuleBookPipe {
    /**
     * Applies the current pipe to the rulebook instance
     * @param rulebook
     */
    void apply(RuleBook rulebook);
}
