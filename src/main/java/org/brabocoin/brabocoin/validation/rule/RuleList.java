package org.brabocoin.brabocoin.validation.rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RuleList {
    private final List<Class<? extends Rule>> rules;

    public RuleList(List<Class<? extends Rule>> rules) {
        this.rules = new ArrayList<>(rules);
    }

    public RuleList(Class<? extends Rule>... rules) {
        this.rules = Arrays.asList(rules);
    }

    public List<Class<? extends Rule>> getRules() {
        return Collections.unmodifiableList(rules);
    }
}