package org.brabocoin.brabocoin.validation.block.rules;

import org.brabocoin.brabocoin.chain.IndexedChain;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.block.BlockRule;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;

public class ContextualTransactionCheckBlkRule extends BlockRule {
    private TransactionValidator transactionValidator;
    private TransactionProcessor transactionProcessor;
    private IndexedChain mainChain;
    private ChainUTXODatabase chainUTXODatabase;
    private Signer signer;

    @Override
    public boolean valid() {
        return block.getTransactions().stream()
                .skip(1)
                .allMatch(t -> transactionValidator.checkTransactionValid(
                        TransactionValidator.RuleLists.BLOCK_CONTEXTUAL,
                        t,
                        consensus,
                        // Note: these facts are not used in the non-contextual transaction rules
                        transactionProcessor, mainChain, null, chainUTXODatabase, signer
                ).isPassed());
    }
}
