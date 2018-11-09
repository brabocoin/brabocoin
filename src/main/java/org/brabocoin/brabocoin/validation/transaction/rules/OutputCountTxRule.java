package org.brabocoin.brabocoin.validation.transaction.rules;

import com.deliveredtechnologies.rulebook.annotation.Rule;
import com.deliveredtechnologies.rulebook.annotation.When;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 *
 * Inputs can only be empty on a coinbase transaction, empty output is never allowed.
 */
@Rule(name = "Input and output count rule")
public class OutputCountTxRule extends TransactionRule {
    @When
    public boolean valid() {
        if (transaction.isCoinbase()) {
            // If coinbase, inputs can be empty, only check outputs
            return transaction.getOutputs().size() > 0;
        } else {
            // else, inputs can not be empty
            return transaction.getInputs().size() > 0 && transaction.getOutputs().size() > 0;
        }
    }
}
