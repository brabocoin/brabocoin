package org.brabocoin.brabocoin.validation;

import org.brabocoin.brabocoin.validation.rule.RuleBookPipe;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public interface Validator<T> extends ValidationListener {
    List<RuleBookPipe> ruleBookPipes = new ArrayList<>();
    ValidationResult run(@NotNull T t, @NotNull RuleList ruleList);

    default void addRuleBookPipe(RuleBookPipe pipe) {
        ruleBookPipes.add(pipe);
    }

    default void removeRuleBookPipe(RuleBookPipe pipe) {
        ruleBookPipes.remove(pipe);
    }
}
