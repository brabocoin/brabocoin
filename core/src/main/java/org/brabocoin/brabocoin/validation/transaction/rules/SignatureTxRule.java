package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.crypto.Signature;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Transaction rule
 * <p>
 * All signatures of the input must be valid.
 */
public class SignatureTxRule extends TransactionRule {

    private ReadonlyUTXOSet utxoSet;

    private Signer signer;

    public boolean isValid() {
        if (transaction.getSignatures().stream().anyMatch(Objects::isNull)) {
            return false;
        }

        return IntStream.range(0, transaction.getInputs().size())
            .allMatch(i -> {
                Input input = transaction.getInputs().get(i);
                Signature signature = transaction.getSignatures().get(i);

                try {
                    return signer.verifySignature(
                        signature,
                        Objects.requireNonNull(utxoSet.findUnspentOutputInfo(input)).getAddress(),
                        transaction.getSignableTransactionData()
                    );
                }
                catch (DatabaseException e) {
                    e.printStackTrace();
                    return false;
                }
            });
    }
}
