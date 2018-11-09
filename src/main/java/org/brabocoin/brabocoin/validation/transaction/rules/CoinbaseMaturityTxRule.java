package org.brabocoin.brabocoin.validation.transaction.rules;

import com.deliveredtechnologies.rulebook.annotation.Given;
import com.deliveredtechnologies.rulebook.annotation.Rule;
import com.deliveredtechnologies.rulebook.annotation.When;
import org.brabocoin.brabocoin.chain.IndexedChain;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

import java.util.Objects;

/**
 * Transaction rule
 *
 * Reject if the spent coinbase it not mature enough.
 */
@Rule(name = "Coinbase maturity rule")
public class CoinbaseMaturityTxRule extends TransactionRule {
    @Given("chainUTXODatabase")
    private ChainUTXODatabase chainUTXODatabase;

    @Given("mainChain")
    private IndexedChain mainChain;

    @When
    public boolean valid() {
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
