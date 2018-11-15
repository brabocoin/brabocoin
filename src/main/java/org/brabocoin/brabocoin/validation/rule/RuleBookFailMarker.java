package org.brabocoin.brabocoin.validation.rule;

public class RuleBookFailMarker {
    private Class failedRule;
    private RuleBookFailMarker child;

    public RuleBookFailMarker(Class failedRule) {
        this.failedRule = failedRule;
    }

    public boolean hasChild(){
        return child != null;
    }

    public RuleBookFailMarker getChild() {
        return child;
    }

    public Class getFailedRule() {
        return failedRule;
    }

    public void setChild(RuleBookFailMarker child) {
        this.child = child;
    }
}
