package org.brabocoin.brabocoin.validation.transaction.rules;

import org.brabocoin.brabocoin.chain.IndexedChain;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.validation.Consensus;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import java.util.Objects;

/**
 * Transaction rule
 */
@Rule(name = "Coinbase maturity rule", description = "Reject if the spent coinbase it not mature enough.")
public class CoinbaseMaturityTxRule {
    @Condition
    public boolean valid(@Fact("transaction") Transaction transaction, @Fact("ChainUTXODatabase") ChainUTXODatabase chainUTXODatabase, @Fact("mainChain") IndexedChain mainChain, @Fact("consensus") Consensus consensus) {
        return transaction.getInputs()
                .stream()
                .map(i -> {
                    try {
                        return chainUTXODatabase.findUnspentOutputInfo(i);
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
