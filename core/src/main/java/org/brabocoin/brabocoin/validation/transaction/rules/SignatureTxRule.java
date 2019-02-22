package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.crypto.Signature;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Transaction rule
 * <p>
 * All signatures of the input must be valid.
 */
@ValidationRule(name = "Valid signatures", failedName = "Transaction contains invalid signature",
                description = "All signatures of the transaction are valid.")
public class SignatureTxRule extends TransactionRule {

    private ReadonlyUTXOSet utxoSet;

    private Signer signer;

    @DescriptionField
    private boolean sigValid;

    public boolean isValid() {
        if (transaction.getSignatures().stream().anyMatch(Objects::isNull)) {
            sigValid = false;
            return sigValid;
        }

        sigValid = IntStream.range(0, transaction.getInputs().size())
            .allMatch(i -> {
                Signature signature = transaction.getSignatures().get(i);

                return signer.verifySignature(
                    signature,
                    transaction.getSignableTransactionData()
                );
            });

        return sigValid;
    }
}
