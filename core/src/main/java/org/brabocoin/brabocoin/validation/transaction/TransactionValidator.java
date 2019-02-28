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
import org.brabocoin.brabocoin.validation.transaction.rules.SignaturePublicKeyTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.SignatureTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.SufficientInputTxRule;
import org.brabocoin.brabocoin.validation.transaction.rules.ValidInputUTXOTxRule;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Validation rules for transactions.
 */
public class TransactionValidator implements Validator {

    private static final Logger LOGGER = Logger.getLogger(TransactionValidator.class.getName());

    public static final RuleList ALL = new RuleList(
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
        SignatureCountTxRule.class,
        SignaturePublicKeyTxRule.class,
        SignatureTxRule.class
    );

    public static final RuleList AFTER_ORPHAN = new RuleList(
        PoolDoubleSpendingTxRule.class,
        ValidInputUTXOTxRule.class,
        CoinbaseMaturityTxRule.class,
        OutputValueTxRule.class,
        InputValueRangeTxRule.class,
        SufficientInputTxRule.class,
        SignaturePublicKeyTxRule.class,
        SignatureTxRule.class
    );

    public static final RuleList BLOCK_NONCONTEXTUAL = new RuleList(
        SignatureCountTxRule.class,
        InputOutputNotEmptyTxRule.class,
        OutputValueTxRule.class,
        SignatureTxRule.class
    );

    public static final RuleList BLOCK_CONTEXTUAL = new RuleList(
        ValidInputUTXOTxRule.class,
        CoinbaseMaturityTxRule.class,
        InputValueRangeTxRule.class,
        SufficientInputTxRule.class,
        SignaturePublicKeyTxRule.class
    );

    public static final RuleList ORPHAN = new RuleList(
        ValidInputUTXOTxRule.class
    );

    private Consensus consensus;
    private IndexedChain mainChain;
    private TransactionPool transactionPool;
    private ReadonlyUTXOSet chainUTXODatabase;
    private UTXODatabase poolUTXODatabase;
    private ReadonlyUTXOSet compositeUTXO;
    private Signer signer;

    /**
     * Construct transaction validator.
     *
     * @param state
     *     The state for the node.
     */
    public TransactionValidator(@NotNull State state) {
        this(
            state.getConsensus(),
            state.getBlockchain().getMainChain(),
            state.getTransactionPool(),
            state.getChainUTXODatabase(),
            state.getPoolUTXODatabase(),
            state.getSigner()
        );
    }

    public TransactionValidator(Consensus consensus,
                                IndexedChain mainChain,
                                TransactionPool transactionPool,
                                ReadonlyUTXOSet chainUTXODatabase,
                                UTXODatabase poolUTXODatabase,
                                Signer signer) {
        this.consensus = consensus;
        this.mainChain = mainChain;
        this.transactionPool = transactionPool;
        this.chainUTXODatabase = chainUTXODatabase;
        this.poolUTXODatabase = poolUTXODatabase;
        this.signer = signer;
        this.compositeUTXO = new CompositeReadonlyUTXOSet(chainUTXODatabase, poolUTXODatabase);
    }

    public TransactionValidator withChainUTXOSet(@NotNull ReadonlyUTXOSet chainUTXOSet) {
        return new TransactionValidator(
            this.consensus,
            this.mainChain,
            this.transactionPool,
            chainUTXOSet,
            this.poolUTXODatabase,
            this.signer
        );
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

    public TransactionValidationResult validate(@NotNull Transaction transaction,
                                                @NotNull RuleList ruleList,
                                                boolean useCompositeUTXO) {
        RuleBook ruleBook = new RuleBook(ruleList);

        ruleBook.addListener(this);

        FactMap factMap = createFactMap(
            transaction,
            useCompositeUTXO ? compositeUTXO : chainUTXODatabase
        );

        validationListeners.forEach(l -> l.onValidationStarted(factMap));

        return TransactionValidationResult.from(ruleBook.run(factMap));
    }

    @Override
    public void onRuleValidation(Rule rule, RuleBookResult result, RuleBook ruleBook) {
        validationListeners.forEach(l -> l.onRuleValidation(rule, result, ruleBook));
    }
}
