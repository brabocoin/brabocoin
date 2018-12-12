package org.brabocoin.brabocoin.validation;

import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.jetbrains.annotations.NotNull;

public interface Validator<T> extends ValidationListener {
    ValidationResult run(@NotNull T t, @NotNull RuleList ruleList);
}
