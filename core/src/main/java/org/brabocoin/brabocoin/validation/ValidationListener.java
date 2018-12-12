package org.brabocoin.brabocoin.validation;

import org.brabocoin.brabocoin.validation.rule.Rule;
import org.brabocoin.brabocoin.validation.rule.RuleBookResult;

/**
 * Listener for validation events
 */
public interface ValidationListener {

    /**
     * Callback on rule validation.
     *
     * @param rule
     *     The rule that was executed
     * @param result
     *     The result of the rule
     */
    default void onRuleValidation(Rule rule, RuleBookResult result) {

    }
}

