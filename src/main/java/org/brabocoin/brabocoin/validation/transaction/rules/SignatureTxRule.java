package org.brabocoin.brabocoin.validation.transaction.rules;

import com.deliveredtechnologies.rulebook.annotation.Given;
import com.deliveredtechnologies.rulebook.annotation.Rule;
import com.deliveredtechnologies.rulebook.annotation.When;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

import java.util.AbstractMap;
import java.util.Objects;

/**
 * Transaction rule
 *
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
        return transaction.getInputs()
                .stream()
                .map(i -> {
                    try {
                        return new AbstractMap.SimpleEntry<>(
                                transactionProcessor.findUnspentOutputInfo(i),
                                i
                        );
                    } catch (DatabaseException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .allMatch(t -> signer.verifySignature(
                        t.getValue().getSignature(),
                        t.getKey().getAddress(),
                        transaction.getSignableTransactionData()
                        ));
    }
}
