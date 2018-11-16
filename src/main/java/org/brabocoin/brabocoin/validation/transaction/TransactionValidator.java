package org.brabocoin.brabocoin.validation.transaction;

import org.brabocoin.brabocoin.chain.IndexedChain;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.dal.CompositeReadonlyUTXOSet;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.dal.UTXODatabase;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.fact.FactMap;
import org.brabocoin.brabocoin.validation.rule.RuleBook;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.brabocoin.brabocoin.validation.transaction.rules.CoinbaseCreationTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.CoinbaseMaturityTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.DuplicateInputTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.DuplicatePoolTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.InputValueTxRange;
import org.brabocoin.brabocoin.validation.transaction.rules.MaxSizeTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.InputOutputNotEmptyTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.OutputValueTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.PoolDoubleSpendingTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.SignatureTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.SufficientInputTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.ValidInputUTXOTxRule;
import org.jetbrains.annotations.NotNull;

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
            InputOutputNotEmptyTxRule.class,
            DuplicateInputTxRule.class,
            PoolDoubleSpendingTxRule.class,
            ValidInputUTXOTxRule.class,
            CoinbaseMaturityTxRule.class,
            OutputValueTxRule.class,
            InputValueTxRange.class,
            SufficientInputTxRule.class,
            SignatureTxRule.class
    );

    private static final RuleList AFTER_ORPHAN = new RuleList(
            ValidInputUTXOTxRule.class,
            CoinbaseMaturityTxRule.class,
            OutputValueTxRule.class,
            InputValueTxRange.class,
            SufficientInputTxRule.class,
            SignatureTxRule.class
    );

    private static final RuleList BLOCK_NONCONTEXTUAL = new RuleList(
            InputOutputNotEmptyTxRule.class,
            OutputValueTxRule.class
    );

    private static final RuleList BLOCK_CONTEXTUAL = new RuleList(
            ValidInputUTXOTxRule.class,
            CoinbaseMaturityTxRule.class,
            InputValueTxRange.class,
            SufficientInputTxRule.class,
            SignatureTxRule.class
    );
    private Consensus consensus;
    private IndexedChain mainChain;
    private TransactionPool transactionPool;
    private ReadonlyUTXOSet chainUTXODatabase;
    private UTXODatabase poolUTXO;
    private ReadonlyUTXOSet compositeUTXO;
    private Signer signer;

    /**
     * Construct transaction validator.
     *
     * @param consensus       Consensus object
     * @param mainChain       The indexed main chain
     * @param transactionPool Transaction pool
     * @param chainUTXO       UTXO database for the main chain
     * @param poolUTXO        Pool UTXO
     * @param signer          Signer object
     */
    public TransactionValidator(
            Consensus consensus,
            IndexedChain mainChain,
            TransactionPool transactionPool,
            ChainUTXODatabase chainUTXO,
            UTXODatabase poolUTXO,
            Signer signer) {
        this.consensus = consensus;
        this.mainChain = mainChain;
        this.transactionPool = transactionPool;
        this.chainUTXODatabase = chainUTXO;
        this.poolUTXO = poolUTXO;
        this.signer = signer;

        this.compositeUTXO = (chainUTXO != null && poolUTXO != null) ? new CompositeReadonlyUTXOSet(chainUTXO, poolUTXO) : null;
    }

    private FactMap createFactMap(@NotNull Transaction transaction, ReadonlyUTXOSet utxoSet) {
        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);
        facts.put("mainChain", mainChain);
        facts.put("transactionPool", transactionPool);
        facts.put("utxoSet", utxoSet);
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
        return TransactionValidationResult.from(new RuleBook(ALL).run(createFactMap(transaction, compositeUTXO)));
    }

    /**
     * Checks whether a transaction is valid after the transaction is no longer orphan.
     *
     * @param transaction The transaction.
     * @return Whether the transaction is valid.
     */
    public TransactionValidationResult checkTransactionPostOrphan(@NotNull Transaction transaction) {
        return TransactionValidationResult.from(new RuleBook(AFTER_ORPHAN).run(createFactMap(transaction, compositeUTXO)));
    }

    /**
     * Perform a non-contextual check whether a transaction is valid in a block.
     *
     * @param transaction The transaction.
     * @return Whether the transaction is valid.
     */
    public TransactionValidationResult checkTransactionBlockNonContextual(@NotNull Transaction transaction) {
        return TransactionValidationResult.from(new RuleBook(BLOCK_NONCONTEXTUAL).run(createFactMap(transaction, chainUTXODatabase)));
    }

    /**
     * Perform a contextual check whether a transaction is valid in a block.
     *
     * @param transaction The transaction.
     * @return Whether the transaction is valid.
     */
    public TransactionValidationResult checkTransactionBlockContextual(@NotNull Transaction transaction) {
        return TransactionValidationResult.from(new RuleBook(BLOCK_CONTEXTUAL).run(createFactMap(transaction, chainUTXODatabase)));
    }
}
