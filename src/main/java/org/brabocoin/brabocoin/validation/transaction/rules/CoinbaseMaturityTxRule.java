package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.chain.IndexedChain;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

import java.util.Objects;

/**
 * Transaction rule
 *
 * Reject if the spent coinbase it not mature enough.
 */
public class CoinbaseMaturityTxRule extends TransactionRule {
    private TransactionProcessor transactionProcessor;
    private IndexedChain mainChain;

    public boolean valid() {
        return transaction.getInputs()
                .stream()
                .map(i -> {
                    try {
                        return transactionProcessor.findUnspentOutputInfo(i);
                    } catch (DatabaseException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(UnspentOutputInfo::isCoinbase)
                .allMatch(u -> u.getBlockHeight() <= mainChain.getHeight() - consensus.getBlockMaturityDepth());
    }
}
