package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

import java.util.HashSet;
import java.util.Set;

/**
 * Transaction rule
 * <p>
 * Transactions can not contain duplicate inputs.
 */
@ValidationRule(name="No duplicate inputs", failedName = "Transaction contains duplicate inputs", description = "The transaction does not contain duplicate inputs.")
public class DuplicateInputTxRule extends TransactionRule {

    @DescriptionField
    private boolean duplicateInputs;

    public boolean isValid() {
        // Check for no duplicates
        Set<Input> seen = new HashSet<>();

        duplicateInputs = transaction.getInputs().stream().allMatch(seen::add);
        return duplicateInputs;
    }
}
