package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 *
 * The amount of the referenced output of the input must be positive,
 * and the sum of these amounts must be smaller than the max transaction range decided by consensus.
 */
public class InputValueTxRange extends TransactionRule {
    private TransactionProcessor transactionProcessor;

    public boolean valid() {
        long sum = 0L;
        for (Input input : transaction.getInputs()) {
            UnspentOutputInfo unspentOutputInfo;
            try {
                unspentOutputInfo = transactionProcessor.findUnspentOutputInfo(input);
            } catch (DatabaseException e) {
                e.printStackTrace();
                return false;
            }

            if (unspentOutputInfo == null) {
                return false;
            }

            if (unspentOutputInfo.getAmount() <= 0) {
                return false;
            }

            sum += unspentOutputInfo.getAmount();
            if (sum > consensus.getMaxMoneyValue()) {
                return false;
            }
        }

        return true;
    }
}
