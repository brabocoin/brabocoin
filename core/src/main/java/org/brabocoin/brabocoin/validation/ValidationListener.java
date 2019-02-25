package org.brabocoin.brabocoin.validation;

import org.brabocoin.brabocoin.validation.fact.FactMap;
import org.brabocoin.brabocoin.validation.rule.Rule;
import org.brabocoin.brabocoin.validation.rule.RuleBook;
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
    default void onRuleValidation(Rule rule, RuleBookResult result, RuleBook ruleBook) {

    }

    /**
     * Callback on validation start.
     *
     * @param facts Facts used for validation.
     */
    default void onValidationStarted(FactMap facts) {

    }
}

