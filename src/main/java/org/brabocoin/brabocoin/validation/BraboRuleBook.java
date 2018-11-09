package org.brabocoin.brabocoin.validation;

import com.deliveredtechnologies.rulebook.model.RuleStatus;
import com.deliveredtechnologies.rulebook.model.runner.RuleBookRunner4PojoClasses;

import java.util.List;

public class BraboRuleBook extends RuleBookRunner4PojoClasses {
    public BraboRuleBook(List<Class<?>> pojoRules) {
        super(pojoRules);

        // Set default success value to true
        setDefaultResult(true);
    }

    public boolean passed() {
        return getRuleStatusMap().values().stream().allMatch(s -> s == RuleStatus.EXECUTED);
    }
}
