package org.brabocoin.brabocoin.validation.transaction;

import org.brabocoin.brabocoin.chain.IndexedChain;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.*;
import org.brabocoin.brabocoin.validation.fact.FactMap;
import org.brabocoin.brabocoin.validation.rule.RuleBook;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.brabocoin.brabocoin.validation.transaction.rules.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Validation rules for transactions.
 */
public class TransactionValidator {
    private static final Logger LOGGER = Logger.getLogger(TransactionValidator.class.getName());

    public static class RuleLists {
        public static final RuleList ALL = new RuleList(
                Arrays.asList(
                        DuplicatePoolTxRule.class,
                        MaxSizeTxRule.class,
                        CoinbaseCreationTxRule.class,
                        OutputCountTxRule.class,
                        DuplicateInputTxRule.class,
                        PoolDoubleSpendingTxRule.class,
                        ValidInputTxRule.class,
                        CoinbaseMaturityTxRule.class,
                        OutputValueTxRule.class,
                        InputValueTxRange.class,
                        SufficientInputTxRule.class,
                        SignatureTxRule.class
                )
        );

        public static final RuleList AFTER_ORPHAN = new RuleList(
                Arrays.asList(
                        ValidInputTxRule.class,
                        CoinbaseMaturityTxRule.class,
                        OutputValueTxRule.class,
                        InputValueTxRange.class,
                        SufficientInputTxRule.class,
                        SignatureTxRule.class
                )
        );

        public static final RuleList BLOCK_NONCONTEXTUAL = new RuleList(
                Arrays.asList(
                        OutputCountTxRule.class,
                        MaxSizeTxRule.class,
                        OutputValueTxRule.class
                )
        );

        public static final RuleList BLOCK_CONTEXTUAL = new RuleList(
                Arrays.asList(
                        ValidInputChainUTXOTxRule.class,
                        CoinbaseMaturityTxRule.class,
                        InputValueTxRange.class,
                        SufficientInputTxRule.class,
                        SignatureTxRule.class
                )
        );
    }

    /**
     * Checks whether a transaction is valid.
     *
     * @param transaction
     *     The transaction.
     * @return Whether the transaction is valid.
     */
    public TransactionValidationResult checkTransactionValid(@NotNull RuleList list,
                                                @NotNull Transaction transaction,
                                                Consensus consensus,
                                                TransactionProcessor transactionProcessor,
                                                IndexedChain mainChain,
                                                TransactionPool transactionPool,
                                                ChainUTXODatabase chainUTXODatabase,
                                                Signer signer) {
        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);
        facts.put("mainChain", mainChain);
        facts.put("transactionProcessor", transactionProcessor);
        facts.put("transactionPool", transactionPool);
        facts.put("chainUTXODatabase", chainUTXODatabase);
        facts.put("signer", signer);

        return new TransactionValidationResult(new RuleBook(list).run(facts));
    }
}
