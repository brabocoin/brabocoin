package org.brabocoin.brabocoin.validation.block;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedChain;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.block.rules.*;
import org.brabocoin.brabocoin.validation.fact.FactMap;
import org.brabocoin.brabocoin.validation.rule.RuleBook;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * @author Sten Wessel
 */
public class BlockValidator {
    private static final Logger LOGGER = Logger.getLogger(BlockValidator.class.getName());

    private static final RuleList INCOMING_BLOCK = new RuleList(
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
    );

    private static final RuleList AFTER_ORPHAN = new RuleList(
            KnownParentBlkRule.class,
            ValidParentBlkRule.class,
            ValidBlockHeightBlkRule.class
    );

    private static final RuleList CONNECT_TO_CHAIN = new RuleList(
            DuplicateCoinbaseBlkRule.class,
            ContextualTransactionCheckBlkRule.class,
            LegalTransactionFeesBlkRule.class,
            ValidCoinbaseOutputAmountBlkRule.class
    );
    private Consensus consensus;
    private TransactionValidator transactionValidator;
    private TransactionProcessor transactionProcessor;
    private IndexedChain mainChain;
    private Blockchain blockchain;
    private ChainUTXODatabase chainUTXODatabase;
    private Signer signer;

    public BlockValidator(
            Consensus consensus,
            TransactionValidator transactionValidator,
            TransactionProcessor transactionProcessor,
            IndexedChain mainChain,
            Blockchain blockchain,
            ChainUTXODatabase chainUTXODatabase,
            Signer signer) {

        this.consensus = consensus;
        this.transactionValidator = transactionValidator;
        this.transactionProcessor = transactionProcessor;
        this.mainChain = mainChain;
        this.blockchain = blockchain;
        this.chainUTXODatabase = chainUTXODatabase;
        this.signer = signer;
    }

    private FactMap createFactMap(@NotNull Block block) {
        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        facts.put("transactionValidator", transactionValidator);
        facts.put("transactionProcessor", transactionProcessor);
        facts.put("mainChain", mainChain);
        facts.put("blockchain", blockchain);
        facts.put("chainUTXODatabase", chainUTXODatabase);
        facts.put("signer", signer);

        return facts;
    }

    public BlockValidationResult checkIncomingBlockValid(@NotNull Block block) {
        return BlockValidationResult.from(new RuleBook(INCOMING_BLOCK).run(createFactMap(block)));
    }

    public BlockValidationResult checkPostOrphanBlockValid(@NotNull Block block) {
        return BlockValidationResult.from(new RuleBook(AFTER_ORPHAN).run(createFactMap(block)));
    }

    public BlockValidationResult checkConnectedOrphanBlockValid(@NotNull Block block) {
        return BlockValidationResult.from(new RuleBook(CONNECT_TO_CHAIN).run(createFactMap(block)));
    }
}
