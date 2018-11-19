package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

import java.util.Objects;

/**
 * Transaction rule
 * <p>
 * All signatures of the input must be valid.
 */
public class SignatureTxRule extends TransactionRule {
    private ReadonlyUTXOSet utxoSet;

    private Signer signer;

    public boolean isValid() {
        if (transaction.getInputs().stream().map(Input::getSignature).anyMatch(Objects::isNull)) {
            return false;
        }

        return transaction.getInputs().stream()
                .allMatch(i -> {
                    try {
                        return signer.verifySignature(
                                Objects.requireNonNull(i.getSignature()),
                                Objects.requireNonNull(utxoSet.findUnspentOutputInfo(i)).getAddress(),
                                transaction.getSignableTransactionData());
                    } catch (DatabaseException e) {
                        e.printStackTrace();
                        return false;
                    }
                });
    }
}
