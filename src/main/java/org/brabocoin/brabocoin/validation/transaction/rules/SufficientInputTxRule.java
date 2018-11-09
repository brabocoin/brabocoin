package org.brabocoin.brabocoin.validation.transaction.rules;

import com.deliveredtechnologies.rulebook.annotation.Given;
import com.deliveredtechnologies.rulebook.annotation.Rule;
import com.deliveredtechnologies.rulebook.annotation.When;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 *
 * The sum of inputs must be greater than the sum of outputs.
 */
@Rule(name = "Valid input amount for the outputs")
public class SufficientInputTxRule extends TransactionRule {
    @Given("transactionProcessor")
    private TransactionProcessor transactionProcessor;

    @When
    public boolean valid() {
        long outputSum = 0L;
        for (Output output : transaction.getOutputs()) {
            outputSum += output.getAmount();
        }

        long inputSum = 0L;
        for (Input input : transaction.getInputs()) {
            UnspentOutputInfo unspentOutputInfo;
            try {
                unspentOutputInfo = transactionProcessor.findUnspentOutputInfo(input);
            } catch (DatabaseException e) {
                e.printStackTrace();
                return false;
            }

            inputSum += unspentOutputInfo.getAmount();
        }

        return inputSum > outputSum;
    }
}
