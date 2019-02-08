package org.brabocoin.brabocoin.gui.control.table;

import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.rule.Rule;

/**
 * Table cell that formats a rule.
 */
public class RuleTableCell<T> extends CopyableTableCell<T, Class<? extends Rule>> {

    @Override
    protected void updateItem(Class<? extends Rule> item, boolean empty) {
        if (empty) {
            setText(null);
        }
        else {
            ValidationRule annotation = item.getAnnotation(ValidationRule.class);
            if (annotation == null) {
                setText(null);
                return;
            }

            setText(annotation.name());
        }
    }
}
