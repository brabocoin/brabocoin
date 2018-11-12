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
        try {
            Rule rule = ruleClass.newInstance();
            List<Field> fields = getAllFields(new ArrayList<>(), ruleClass);
            // Set all fields accessible
            fields.forEach(f -> f.setAccessible(true));

            for (Map.Entry<String, Object> objectEntry : facts.entrySet()) {
                Field foundField = fields.stream()
                        .filter(f -> f.getName().equals(objectEntry.getKey()))
                        .findFirst()
                        .orElse(null);
                if (foundField != null) {
                    foundField.set(rule, objectEntry.getValue());
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
