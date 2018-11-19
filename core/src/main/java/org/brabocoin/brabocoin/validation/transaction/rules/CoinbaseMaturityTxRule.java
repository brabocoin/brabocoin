package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.chain.IndexedChain;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

import java.util.Objects;

import static org.brabocoin.brabocoin.util.LambdaExceptionUtil.rethrowFunction;

/**
 * Transaction rule
 *
 * Reject if the spent coinbase it not mature enough.
 */
public class CoinbaseMaturityTxRule extends TransactionRule {
    private ReadonlyUTXOSet utxoSet;
    private IndexedChain mainChain;

    public boolean isValid() {
        try {
            return transaction.getInputs()
                    .stream()
                    .map(rethrowFunction(utxoSet::findUnspentOutputInfo))
                    .filter(Objects::nonNull)
                    .filter(UnspentOutputInfo::isCoinbase)
                    .allMatch(u -> u.getBlockHeight() <= mainChain.getHeight() - consensus.getCoinbaseMaturityDepth());
        } catch (DatabaseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
