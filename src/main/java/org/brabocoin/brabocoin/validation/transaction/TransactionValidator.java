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

    private static final RuleList ALL = new RuleList(
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
    );

    private static final RuleList AFTER_ORPHAN = new RuleList(
            ValidInputTxRule.class,
            CoinbaseMaturityTxRule.class,
            OutputValueTxRule.class,
            InputValueTxRange.class,
            SufficientInputTxRule.class,
            SignatureTxRule.class
    );

    private static final RuleList BLOCK_NONCONTEXTUAL = new RuleList(
            OutputCountTxRule.class,
            MaxSizeTxRule.class,
            OutputValueTxRule.class
    );

    private static final RuleList BLOCK_CONTEXTUAL = new RuleList(
            Arrays.asList(
                    ValidInputChainUTXOTxRule.class,
                    CoinbaseMaturityTxRule.class,
                    InputValueTxRange.class,
                    SufficientInputTxRule.class,
                    SignatureTxRule.class
            )
    );
    private Consensus consensus;
    private TransactionProcessor transactionProcessor;
    private IndexedChain mainChain;
    private TransactionPool transactionPool;
    private ChainUTXODatabase chainUTXODatabase;
    private Signer signer;

    /**
     * Construct transaction validator.
     *
     * @param consensus            Consensus object
     * @param transactionProcessor Transaction processor
     * @param mainChain            The indexed main chain
     * @param transactionPool      Transaction pool
     * @param chainUTXODatabase    UTXO database for the main chian
     * @param signer               Signer object
     */
    public TransactionValidator(
            Consensus consensus,
            TransactionProcessor transactionProcessor,
            IndexedChain mainChain,
            TransactionPool transactionPool,
            ChainUTXODatabase chainUTXODatabase,
            Signer signer) {
        this.consensus = consensus;
        this.transactionProcessor = transactionProcessor;
        this.mainChain = mainChain;
        this.transactionPool = transactionPool;
        this.chainUTXODatabase = chainUTXODatabase;
        this.signer = signer;
    }

    private FactMap createFactMap(@NotNull Transaction transaction) {
        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);
        facts.put("mainChain", mainChain);
        facts.put("transactionProcessor", transactionProcessor);
        facts.put("transactionPool", transactionPool);
        facts.put("chainUTXODatabase", chainUTXODatabase);
        facts.put("signer", signer);

        return facts;
    }

    /**
     * Checks whether a transaction is valid using all known transaction rules.
     *
     * @param transaction The transaction.
     * @return Whether the transaction is valid.
     */
    public TransactionValidationResult checkTransactionValid(@NotNull Transaction transaction) {
        return TransactionValidationResult.from(new RuleBook(ALL).run(createFactMap(transaction)));
    }

    /**
     * Checks whether a transaction is valid after the transaction is no longer orphan.
     *
     * @param transaction The transaction.
     * @return Whether the transaction is valid.
     */
    public TransactionValidationResult checkTransactionPostOrphan(@NotNull Transaction transaction) {
        return TransactionValidationResult.from(new RuleBook(AFTER_ORPHAN).run(createFactMap(transaction)));
    }

    /**
     * Perform a non-contextual check whether a transaction is valid in a block.
     *
     * @param transaction The transaction.
     * @return Whether the transaction is valid.
     */
    public TransactionValidationResult checkTransactionBlockNonContextual(@NotNull Transaction transaction) {
        return TransactionValidationResult.from(new RuleBook(BLOCK_NONCONTEXTUAL).run(createFactMap(transaction)));
    }

    /**
     * Perform a contextual check whether a transaction is valid in a block.
     *
     * @param transaction The transaction.
     * @return Whether the transaction is valid.
     */
    public TransactionValidationResult checkTransactionBlockContextual(@NotNull Transaction transaction) {
        return TransactionValidationResult.from(new RuleBook(BLOCK_CONTEXTUAL).run(createFactMap(transaction)));
    }
}
