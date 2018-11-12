package org.brabocoin.brabocoin.validation.transaction;

import com.deliveredtechnologies.rulebook.FactMap;
import com.deliveredtechnologies.rulebook.NameValueReferableMap;
import org.brabocoin.brabocoin.chain.IndexedChain;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.BraboRuleBook;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.RuleBookResult;
import org.brabocoin.brabocoin.validation.transaction.rules.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Validation rules for transactions.
 */
public class TransactionValidator {
    private static final Logger LOGGER = Logger.getLogger(TransactionValidator.class.getName());

    static class RuleLists {
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
    public RuleBookResult checkTransactionValid(@NotNull Transaction transaction,
                                                @NotNull RuleList list,
                                                @NotNull Consensus consensus,
                                                @NotNull TransactionProcessor transactionProcessor,
                                                @NotNull IndexedChain mainChain,
                                                @NotNull TransactionPool pool,
                                                @NotNull Signer signer) {
        BraboRuleBook ruleBook = new BraboRuleBook(
                list.getRules()
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("mainChain", mainChain);
        facts.setValue("transactionProcessor", transactionProcessor);
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);
        facts.setValue("pool", pool);
        facts.setValue("signer", signer);

        ruleBook.run(facts);
        return ruleBook.getRuleBookResult();
    }

    public static class RuleList {
        private List<Class<?>> rules;

        public RuleList(List<Class<?>> rules) {
            this.rules = rules;
        }

        public List<Class<?>> getRules() {
            return rules;
        }
    }
}
