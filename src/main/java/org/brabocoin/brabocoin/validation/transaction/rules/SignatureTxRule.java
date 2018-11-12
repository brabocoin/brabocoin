package org.brabocoin.brabocoin.validation.transaction.rules;

import com.deliveredtechnologies.rulebook.annotation.Given;
import com.deliveredtechnologies.rulebook.annotation.Rule;
import com.deliveredtechnologies.rulebook.annotation.When;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

import java.util.Objects;

/**
 * Transaction rule
 * <p>
 * All signatures of the input must be valid.
 */
@Rule(name = "Signature rule")
public class SignatureTxRule extends TransactionRule {
    @Given("transactionProcessor")
    private TransactionProcessor transactionProcessor;

    @Given("signer")
    private Signer signer;

    @When
    public boolean valid() {
        if (transaction.getInputs().stream().map(Input::getSignature).anyMatch(Objects::isNull)) {
            return false;
        }

        return transaction.getInputs().stream()
                .allMatch(i -> {
                    try {
                        return signer.verifySignature(
                                Objects.requireNonNull(i.getSignature()),
                                transactionProcessor.findUnspentOutputInfo(i).getAddress(),
                                transaction.getSignableTransactionData());
                    } catch (DatabaseException e) {
                        e.printStackTrace();
                        return false;
                    }
                });
    }
}
