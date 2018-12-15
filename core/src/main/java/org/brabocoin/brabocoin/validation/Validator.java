package org.brabocoin.brabocoin.validation;

import org.brabocoin.brabocoin.validation.rule.RuleBookPipe;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.jetbrains.annotations.NotNull;

public interface Validator<T> extends ValidationListener {
    ValidationResult run(@NotNull T t, @NotNull RuleList ruleList);

    void addRuleBookPipe(RuleBookPipe pipe);

    void removeRuleBookPIpe(RuleBookPipe pipe);
}
