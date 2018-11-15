package org.brabocoin.brabocoin.validation.block;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedChain;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.*;
import org.brabocoin.brabocoin.validation.block.rules.*;
import org.brabocoin.brabocoin.validation.fact.FactMap;
import org.brabocoin.brabocoin.validation.rule.RuleBook;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * @author Sten Wessel
 */
public class BlockValidator {
    private static final Logger LOGGER = Logger.getLogger(BlockValidator.class.getName());

    public static class RuleLists {
        public static final RuleList INCOMING_BLOCK = new RuleList(
                Arrays.asList(
                        MaxNonceBlkRule.class,
                        MaxSizeBlkRule.class,
                        DuplicateStorageBlkRule.class,
                        SatisfiesTargetValueBlkRule.class,
                        CorrectTargetValueBlkRule.class,
                        NonEmptyTransactionListBlkRule.class,
                        HasCoinbaseBlkRule.class,
                        HasSingleCoinbaseBlkRule.class,
                        ValidMerkleRootBlkRule.class,
                        NonContextualTransactionCheckBlkRule.class,
                        KnownParentBlkRule.class,
                        ValidParentBlkRule.class,
                        ValidBlockHeightBlkRule.class
                )
        );

        public static final RuleList AFTER_ORPHAN = new RuleList(
                Arrays.asList(
                        KnownParentBlkRule.class,
                        ValidParentBlkRule.class,
                        ValidBlockHeightBlkRule.class
                )
        );

        public static final RuleList CONNECT_TO_CHAIN = new RuleList(
                Arrays.asList(
                        DuplicateCoinbaseBlkRule.class,
                        ContextualTransactionCheckBlkRule.class,
                        LegalTransactionFeesBlkRule.class,
                        ValidCoinbaseOutputAmountBlkRule.class
                )
        );
    }

    public BlockValidationResult checkBlockValid(@NotNull RuleList list,
                                   @NotNull Block block,
                                   Consensus consensus,
                                   TransactionValidator transactionValidator,
                                   TransactionProcessor transactionProcessor,
                                   IndexedChain mainChain,
                                   Blockchain blockchain,
                                   ChainUTXODatabase chainUTXODatabase,
                                   Signer signer) {

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        facts.put("transactionValidator", transactionValidator);
        facts.put("transactionProcessor", transactionProcessor);
        facts.put("mainChain", mainChain);
        facts.put("blockchain", blockchain);
        facts.put("chainUTXODatabase", chainUTXODatabase);
        facts.put("signer", signer);

        return new BlockValidationResult(new RuleBook(list).run(facts));
    }
}
