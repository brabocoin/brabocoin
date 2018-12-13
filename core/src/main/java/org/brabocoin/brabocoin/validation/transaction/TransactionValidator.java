package org.brabocoin.brabocoin.validation.transaction;

import org.brabocoin.brabocoin.chain.IndexedChain;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.CompositeReadonlyUTXOSet;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.dal.UTXODatabase;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.ValidationListener;
import org.brabocoin.brabocoin.validation.Validator;
import org.brabocoin.brabocoin.validation.fact.FactMap;
import org.brabocoin.brabocoin.validation.rule.Rule;
import org.brabocoin.brabocoin.validation.rule.RuleBook;
import org.brabocoin.brabocoin.validation.rule.RuleBookResult;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.brabocoin.brabocoin.validation.transaction.rules.CoinbaseCreationTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.CoinbaseMaturityTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.DuplicateInputTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.DuplicatePoolTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.InputOutputNotEmptyTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.InputValueRangeTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.MaxSizeTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.OutputValueTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.PoolDoubleSpendingTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.SignatureCountTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.SignatureTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.SufficientInputTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.ValidInputUTXOTxRule;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Validation rules for transactions.
 */
public class TransactionValidator implements Validator<Transaction> {


    private final List<ValidationListener> validationListeners;
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
        InputValueRangeTxRule.class,
        SufficientInputTxRule.class,
        SignatureTxRule.class
    );

    private static final RuleList AFTER_ORPHAN = new RuleList(
        PoolDoubleSpendingTxRule.class,
        ValidInputUTXOTxRule.class,
        CoinbaseMaturityTxRule.class,
        OutputValueTxRule.class,
        InputValueRangeTxRule.class,
        SufficientInputTxRule.class,
        SignatureTxRule.class
    );

    public static final RuleList BLOCK_NONCONTEXTUAL = new RuleList(
        SignatureCountTxRule.class,
        InputOutputNotEmptyTxRule.class,
        OutputValueTxRule.class
    );

    private static final RuleList BLOCK_CONTEXTUAL = new RuleList(
        ValidInputUTXOTxRule.class,
        CoinbaseMaturityTxRule.class,
        InputValueRangeTxRule.class,
        SufficientInputTxRule.class,
        SignatureTxRule.class
    );

    private static final RuleList ORPHAN = new RuleList(
        ValidInputUTXOTxRule.class
    );

    private Consensus consensus;
    private IndexedChain mainChain;
    private TransactionPool transactionPool;
    private ReadonlyUTXOSet chainUTXODatabase;
    private UTXODatabase poolUTXODatabase;
    private ReadonlyUTXOSet compositeUTXO;
    private Signer signer;

    private ReadonlyUTXOSet currentUTXO;

    /**
     * Construct transaction validator.
     *
     * @param state
     *     The state for the node.
     */
    public TransactionValidator(@NotNull State state) {
        this.consensus = state.getConsensus();
        this.mainChain = state.getBlockchain().getMainChain();
        this.transactionPool = state.getTransactionPool();
        this.chainUTXODatabase = state.getChainUTXODatabase();
        this.poolUTXODatabase = state.getPoolUTXODatabase();
        this.signer = state.getSigner();
        this.compositeUTXO = new CompositeReadonlyUTXOSet(chainUTXODatabase, poolUTXODatabase);
        validationListeners = new ArrayList<>();
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
     * @param transaction
     *     The transaction.
     * @return Whether the transaction is valid.
     */
    public TransactionValidationResult checkTransactionValid(@NotNull Transaction transaction) {
        currentUTXO = compositeUTXO;
        return run(transaction, ALL);
    }

    /**
     * Checks whether a transaction is valid after the transaction is no longer orphan.
     *
     * @param transaction
     *     The transaction.
     * @return Whether the transaction is valid.
     */
    public TransactionValidationResult checkTransactionPostOrphan(
        @NotNull Transaction transaction) {
        currentUTXO = compositeUTXO;
        return run(transaction, AFTER_ORPHAN);
    }

    /**
     * Check whether a transaction is orphan.
     *
     * @param transaction
     *     The transaction to check.
     * @return Whether the transaction is orphan.
     */
    public TransactionValidationResult checkTransactionOrphan(@NotNull Transaction transaction) {
        currentUTXO = compositeUTXO;
        return run(transaction, ORPHAN);
    }

    /**
     * Check whether a transaction is independent.
     *
     * @param transaction
     *     The transaction to check.
     * @return Whether the transaction is independent.
     */
    public TransactionValidationResult checkTransactionIndependent(
        @NotNull Transaction transaction) {
        currentUTXO = chainUTXODatabase;
        return run(transaction, ORPHAN);
    }

    /**
     * Perform a non-contextual check whether a transaction is valid in a block.
     *
     * @param transaction
     *     The transaction.
     * @return Whether the transaction is valid.
     */
    public TransactionValidationResult checkTransactionBlockNonContextual(
        @NotNull Transaction transaction) {
        currentUTXO = chainUTXODatabase;
        return run(transaction, BLOCK_NONCONTEXTUAL);
    }

    /**
     * Perform a contextual check whether a transaction is valid in a block.
     *
     * @param transaction
     *     The transaction.
     * @return Whether the transaction is valid.
     */
    public TransactionValidationResult checkTransactionBlockContextual(
        @NotNull Transaction transaction) {
        currentUTXO = chainUTXODatabase;
        return run(transaction, BLOCK_CONTEXTUAL);
    }

    @Override
    public TransactionValidationResult run(@NotNull Transaction transaction, @NotNull RuleList ruleList) {
        RuleBook ruleBook = new RuleBook(ruleList);

        ruleBook.addListener(this);

        return TransactionValidationResult.from(ruleBook.run(createFactMap(transaction, currentUTXO)));
    }

    @Override
    public void onRuleValidation(Rule rule, RuleBookResult result) {
        validationListeners.forEach(l -> l.onRuleValidation(rule, result));
    }
}
