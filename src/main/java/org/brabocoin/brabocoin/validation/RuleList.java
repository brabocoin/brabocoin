package org.brabocoin.brabocoin.validation;

import java.util.List;

public class RuleList {
    private List<Class<? extends Rule>> rules;

    public RuleList(List<Class<? extends Rule>> rules) {
        this.rules = rules;
    }

    public List<Class<? extends Rule>> getRules() {
        return rules;
    }
}