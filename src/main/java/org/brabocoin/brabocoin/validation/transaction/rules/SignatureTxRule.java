package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import java.util.AbstractMap;
import java.util.Objects;

/**
 * Transaction rule
 */
@Rule(name = "Signature rule", description = "All signatures of the input must be valid.")
public class SignatureTxRule {
    @Condition
    public boolean valid(@Fact("transaction") Transaction transaction, @Fact("transactionProcessor") TransactionProcessor transactionProcessor, @Fact("signer") Signer signer) {
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
