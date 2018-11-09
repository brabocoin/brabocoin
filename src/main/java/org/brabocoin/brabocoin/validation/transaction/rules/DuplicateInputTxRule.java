package org.brabocoin.brabocoin.validation.transaction.rules;

import com.deliveredtechnologies.rulebook.annotation.Rule;
import com.deliveredtechnologies.rulebook.annotation.When;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

import java.util.HashSet;
import java.util.List;

/**
 * Transaction rule
 *
 * Transactions can not contain duplicate inputs.
 */
@Rule(name = "Duplicate input rule")
public class DuplicateInputTxRule extends TransactionRule {
    @When
    public boolean valid() {
        // Check for no duplicates
        List<Input> inputs = transaction.getInputs();
        return inputs.size() == new HashSet<>(inputs).size();
    }
}
