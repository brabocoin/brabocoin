package org.brabocoin.brabocoin.validation.rule;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class RuleList implements Iterable<Class<? extends Rule>> {

    private final List<Class<? extends Rule>> rules;

    public RuleList(List<Class<? extends Rule>> rules) {
        this.rules = new ArrayList<>(rules);
    }

    public RuleList(Class<? extends Rule>... rules) {
        this.rules = Arrays.asList(rules);
    }

    public RuleList(RuleList... ruleLists) {
        rules = new ArrayList<>();
        for (RuleList list : ruleLists) {
            rules.addAll(list.getRules());
        }
    }

    public List<Class<? extends Rule>> getRules() {
        return new ArrayList<>(rules);
    }

    public Class[] toArray() {
        return rules.toArray(new Class[rules.size()]);
    }

    @NotNull
    @Override
    public Iterator<Class<? extends Rule>> iterator() {
        return rules.iterator();
    }
}
