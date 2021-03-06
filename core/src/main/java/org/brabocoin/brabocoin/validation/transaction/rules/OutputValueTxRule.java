package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 * <p>
 * Output values must be positive and the sum must be smaller than the max transaction range
 * decided by consensus.
 */
@ValidationRule(name = "Legal output value", failedName = "Value of sum of outputs is illegal",
                description = "The sum of all outputs is within the allowed range, and does not "
                    + "overflow.")
public class OutputValueTxRule extends TransactionRule {

    @DescriptionField
    private long maxMoneyValue;
    @DescriptionField
    private long sum;

    public boolean isValid() {
        maxMoneyValue = consensus.getMaxMoneyValue();
        sum = 0L;
        for (Output output : transaction.getOutputs()) {
            if (output.getAmount() <= 0) {
                return false;
            }

            try {
                sum = Math.addExact(sum, output.getAmount());
            }
            catch (ArithmeticException e) {
                // Sum overflows long type
                return false;
            }

            if (sum > maxMoneyValue) {
                return false;
            }
        }

        return true;
    }
}
