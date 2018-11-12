package org.brabocoin.brabocoin.validation;

import com.deliveredtechnologies.rulebook.model.RuleStatus;
import com.deliveredtechnologies.rulebook.model.runner.RuleBookRunner4PojoClasses;

import java.util.List;
import java.util.Map;

public class BraboRuleBook extends RuleBookRunner4PojoClasses {
    public BraboRuleBook(List<Class<?>> pojoRules) {
        super(pojoRules);

        // Set default success value to true
        setDefaultResult(true);
    }

    /**
     * Get whether all rules in this rule book passed.
     *
     * @return Whether all rules in this rule book passed.
     */
    public boolean passed() {
        return getRuleStatusMap().values().stream().allMatch(s -> s == RuleStatus.EXECUTED);
    }

    /**
     * Gets the rule book result for this rule.
     *
     * @return rule book result for this rule.
     */
    public RuleBookResult getRuleBookResult() {
        boolean passed = passed();

        if (!passed) {
            String firstFailedRuleName = getRuleStatusMap().entrySet().stream()
                    .filter(s -> s.getValue() != RuleStatus.EXECUTED)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            if (firstFailedRuleName == null) {
                throw new IllegalStateException("Did not pass rulebook, but could not get failed rule name.");
            }

            Class<?> failedRule = getPojoRules().stream()
                    .filter(r -> r.getSimpleName().equals(firstFailedRuleName))
                    .findFirst()
                    .orElse(null);

            if (failedRule == null) {
                throw new IllegalStateException("Did not pass rulebook, but could not get failed rule class.");
            }

            return new RuleBookResult(passed, failedRule);
        }

        return new RuleBookResult(passed);
    }
}
