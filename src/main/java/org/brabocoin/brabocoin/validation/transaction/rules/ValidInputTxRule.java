package org.brabocoin.brabocoin.validation.transaction.rules;

import com.deliveredtechnologies.rulebook.annotation.Given;
import com.deliveredtechnologies.rulebook.annotation.Rule;
import com.deliveredtechnologies.rulebook.annotation.When;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.processor.ProcessedTransactionStatus;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

/**
 * Transaction rule
 *
 * The inputs used in the transaction must be valid.
 */
@Rule(name = "Valid input rule")
public class ValidInputTxRule extends TransactionRule {
    @Given("transactionProcessor")
    private TransactionProcessor transactionProcessor;

    @When
    public boolean valid() {
        try {
            if (transactionProcessor.checkInputs(transaction) == ProcessedTransactionStatus.ORPHAN) {
                return false;
            }
        } catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
