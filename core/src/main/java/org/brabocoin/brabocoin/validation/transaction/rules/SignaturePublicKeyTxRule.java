package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.crypto.Signature;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Transaction rule
 * <p>
 * All signatures of the transaction must contain the right public key
 */
@ValidationRule(name="Valid public keys in signatures", failedName = "Transaction contains signature with the wrong public key", description = "All signatures of the transaction have the right public key.")
public class SignaturePublicKeyTxRule extends TransactionRule {

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
                    return signer.verifySignaturePublicKey(
                        signature,
                        Objects.requireNonNull(utxoSet.findUnspentOutputInfo(input)).getAddress()
                    );
                }
                catch (DatabaseException e) {
                    e.printStackTrace();
                    return false;
                }
            });
    }
}
