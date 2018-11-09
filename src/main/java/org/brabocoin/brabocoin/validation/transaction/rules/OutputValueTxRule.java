package org.brabocoin.brabocoin.validation.transaction.rules;

import com.deliveredtechnologies.rulebook.annotation.Rule;
import com.deliveredtechnologies.rulebook.annotation.When;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 *
 * Output values must be positive and the sum must be smaller than the max transaction range decided by consensus.
 */
@Rule(name = "Output value rule")
public class OutputValueTxRule extends TransactionRule {
    @When
    public boolean valid() {
        long sum = 0L;
        for (Output output : transaction.getOutputs()) {
            if (output.getAmount() <= 0) {
                return false;
            }

            sum += output.getAmount();
            if (sum > consensus.getMaxTransactionRange()) {
                return false;
            }
        }

        return true;
    }
}
