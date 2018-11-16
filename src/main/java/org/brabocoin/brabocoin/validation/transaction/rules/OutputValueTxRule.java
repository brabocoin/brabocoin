package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 *
 * Output values must be positive and the sum must be smaller than the max transaction range decided by consensus.
 */
public class OutputValueTxRule extends TransactionRule {
    public boolean isValid() {
        long sum = 0L;
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

            if (sum > consensus.getMaxMoneyValue()) {
                return false;
            }
        }

        return true;
    }
}
