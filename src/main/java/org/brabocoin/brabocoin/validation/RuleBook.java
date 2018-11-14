package org.brabocoin.brabocoin.validation;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RuleBook {
    private static final Logger LOGGER = Logger.getLogger(RuleBook.class.getName());

    private RuleList ruleList;

    public RuleBook(RuleList ruleList) {
        this.ruleList = ruleList;
    }

    public RuleBookResult run(FactMap facts) {
        for (Rule rule : getInstantiatedRules(facts)) {
            try {
                if (!rule.valid()) {
                    return new RuleBookResult(false, rule.getClass());
                }
            } catch (NullPointerException e) {
                LOGGER.log(Level.SEVERE, "Fact missing in class, {0}", rule.getClass().getName());
                return new RuleBookResult(false, rule.getClass());
            }
        }

        return new RuleBookResult(true);
    }

    private List<Rule> getInstantiatedRules(FactMap facts) {
        return ruleList.getRules().stream()
                .map(r -> getInstantiatedRule(r, facts))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

    }

    private Rule getInstantiatedRule(Class<? extends Rule> ruleClass, FactMap facts) {
        if (facts.values().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Facts contained null instance.");
        }

        try {
            Rule rule = ruleClass.newInstance();
            List<Field> fields = getAllFields(new ArrayList<>(), ruleClass);
            // Set all fields accessible
            fields.forEach(f -> f.setAccessible(true));

            for (Field field : fields) {
                Object fieldValue = facts.entrySet().stream()
                        .filter(f -> f.getKey().equals(field.getName()))
                        .findFirst()
                        .map(Map.Entry::getValue)
                        .orElse(null);
                if (fieldValue != null) {
                    if (fieldValue instanceof UninitializedFact) {
                        throw new IllegalStateException("Fact found that is uninitialized.");
                    }
                    field.set(rule, fieldValue);
                } else {
                    throw new IllegalStateException(String.format("Could not find field {0} in fact map.", field.getName()));
                }
            }

            return rule;
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }
}
