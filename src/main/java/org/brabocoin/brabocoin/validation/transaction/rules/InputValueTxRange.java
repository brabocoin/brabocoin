package org.brabocoin.brabocoin.validation.transaction.rules;

import com.deliveredtechnologies.rulebook.annotation.Given;
import com.deliveredtechnologies.rulebook.annotation.Rule;
import com.deliveredtechnologies.rulebook.annotation.When;
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
@Rule(name = "Input value rule")
public class InputValueTxRange extends TransactionRule {
    @Given("transactionProcessor")
    private TransactionProcessor transactionProcessor;

    @When
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
            if (sum > consensus.getMaxTransactionRange()) {
                return false;
            }
        }

        return true;
    }
}
