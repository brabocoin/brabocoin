package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.chain.IndexedChain;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.validation.annotation.DescriptionField;
import org.brabocoin.brabocoin.validation.annotation.ValidationRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

import java.util.Objects;

import static org.brabocoin.brabocoin.util.LambdaExceptionUtil.rethrowFunction;

/**
 * Transaction rule
 * <p>
 * Reject if the spent coinbase it not mature enough.
 */
@ValidationRule(name="Coinbase maturity check", description = "All coinbase transactions defined by this transaction's inputs have a depth larger than the coinbase maturity depth defined in consensus.")
public class CoinbaseMaturityTxRule extends TransactionRule {

    private ReadonlyUTXOSet utxoSet;
    private IndexedChain mainChain;

    @DescriptionField


    public boolean isValid() {
        try {
            return transaction.getInputs().stream()
                .map(rethrowFunction(utxoSet::findUnspentOutputInfo))
                .filter(Objects::nonNull)
                .noneMatch(u -> consensus.immatureCoinbase(mainChain.getHeight(), u));
        }
        catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
