package org.brabocoin.brabocoin.validation.rule;

import org.jetbrains.annotations.Nullable;

public class RuleBookFailMarker {
    private final Class<? extends Rule> failedRule;

    @Nullable
    private final RuleBookFailMarker child;

    public RuleBookFailMarker(Class<? extends Rule> failedRule) {
        this.failedRule = failedRule;
        this.child = null;
    }

    public RuleBookFailMarker(Class<? extends Rule> failedRule, @Nullable RuleBookFailMarker child) {
        this.failedRule = failedRule;
        this.child = child;
    }

    public boolean hasChild(){
        return child != null;
    }

    public RuleBookFailMarker getChild() {
        return child;
    }

    public Class<? extends Rule> getFailedRule() {
        return failedRule;
    }
}
