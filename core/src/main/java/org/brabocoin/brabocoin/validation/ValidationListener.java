package org.brabocoin.brabocoin.validation;

import org.brabocoin.brabocoin.validation.rule.Rule;
import org.brabocoin.brabocoin.validation.rule.RuleBookResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener for validation events
 */
public interface ValidationListener {

    List<ValidationListener> validationListeners = new ArrayList<>();

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


    /**
     * Add a listener to validation events.
     *
     * @param listener The listener to add.
     */
    default void addListener(@NotNull ValidationListener listener) {
        this.validationListeners.add(listener);
    }

    /**
     * Remove a listener to validation events.
     *
     * @param listener The listener to remove.
     */
    default void removeListener(@NotNull ValidationListener listener) {
        this.validationListeners.remove(listener);
    }
}

