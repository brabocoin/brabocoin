package org.brabocoin.brabocoin.validation.rule;

import org.brabocoin.brabocoin.validation.fact.FactMap;
import org.brabocoin.brabocoin.validation.fact.CompositeRuleFailMarker;
import org.brabocoin.brabocoin.validation.fact.UninitializedFact;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RuleBook {
    private static final Logger LOGGER = Logger.getLogger(RuleBook.class.getName());

    private final RuleList ruleList;

    public RuleBook(RuleList ruleList) {
        this.ruleList = ruleList;
    }

    public RuleBookResult run(FactMap facts) {
        for (Rule rule : getInstantiatedRules(facts)) {
            boolean passedRule = true;

            try {
                if (!rule.isValid()) {
                    passedRule = false;
                }
            } catch (NullPointerException e) {
                LOGGER.log(Level.SEVERE, "Rule failed: {0}", e.getMessage());
                passedRule = false;
            }

            if (!passedRule) {
                RuleBookFailMarker marker = new RuleBookFailMarker(rule.getClass(), getChild(rule));
                return RuleBookResult.failed(marker);
            }
        }

        return RuleBookResult.passed();
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

            for (Field field : fields) {
                field.setAccessible(true);
                boolean hasChildRuleFailMarkerFlag = field.getAnnotation(CompositeRuleFailMarker.class) != null;
                if (hasChildRuleFailMarkerFlag) {
                    continue;
                }

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
                    throw new IllegalStateException(MessageFormat.format("Could not find field {0} in fact map for rule {1}.", field.getName(), rule.getClass().getName()));
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

    /**
     * Gets the child rulebook fail marker if present, {@code null} otherwise.
     *
     * @return Child RuleBookFailMarker or null if not present.
     */
    private RuleBookFailMarker getChild(Rule rule) {
        List<Field> fields = getAllFields(new ArrayList<>(), rule.getClass());

        RuleBookFailMarker failMarker = null;

        for (Field field : fields) {
            boolean hasChildFailMarker = !Objects.isNull(field.getAnnotation(CompositeRuleFailMarker.class));
            if (!hasChildFailMarker) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object marker = field.get(rule);
                if (marker instanceof RuleBookFailMarker) {
                    failMarker = (RuleBookFailMarker) marker;
                    break;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return failMarker;
    }
}
