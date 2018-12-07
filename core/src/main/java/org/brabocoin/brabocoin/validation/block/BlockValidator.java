package org.brabocoin.brabocoin.validation.block;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.block.rules.ContextualTransactionCheckBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.CorrectTargetValueBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.DuplicateInputBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.DuplicateStorageBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.HasCoinbaseBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.HasSingleCoinbaseBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.KnownParentBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.LegalTransactionFeesBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.MaxNonceBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.MaxSizeBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.NonContextualTransactionCheckBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.NonEmptyTransactionListBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.SatisfiesTargetValueBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.UniqueUnspentCoinbaseBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.ValidBlockHeightBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.ValidCoinbaseBlockHeightBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.ValidCoinbaseOutputAmountBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.ValidMerkleRootBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.ValidNetworkIdBlkRule;
import org.brabocoin.brabocoin.validation.block.rules.ValidParentBlkRule;
import org.brabocoin.brabocoin.validation.fact.FactMap;
import org.brabocoin.brabocoin.validation.rule.RuleBook;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Validates blocks.
 */
public class BlockValidator {

    private static final Logger LOGGER = Logger.getLogger(BlockValidator.class.getName());

    private static final RuleList INCOMING_BLOCK = new RuleList(
        MaxNonceBlkRule.class,
        MaxSizeBlkRule.class,
        ValidNetworkIdBlkRule.class,
        DuplicateStorageBlkRule.class,
        SatisfiesTargetValueBlkRule.class,
        CorrectTargetValueBlkRule.class,
        NonEmptyTransactionListBlkRule.class,
        HasCoinbaseBlkRule.class,
        HasSingleCoinbaseBlkRule.class,
        ValidMerkleRootBlkRule.class,
        NonContextualTransactionCheckBlkRule.class,
        DuplicateInputBlkRule.class,
        KnownParentBlkRule.class,
        ValidParentBlkRule.class,
        ValidBlockHeightBlkRule.class,
        ValidCoinbaseBlockHeightBlkRule.class
    );

    private static final RuleList AFTER_ORPHAN = new RuleList(
        KnownParentBlkRule.class,
        ValidParentBlkRule.class,
        ValidBlockHeightBlkRule.class,
        ValidCoinbaseBlockHeightBlkRule.class
    );

    private static final RuleList CONNECT_TO_CHAIN = new RuleList(
        UniqueUnspentCoinbaseBlkRule.class,
        ContextualTransactionCheckBlkRule.class,
        LegalTransactionFeesBlkRule.class,
        ValidCoinbaseOutputAmountBlkRule.class
    );

    private Consensus consensus;
    private TransactionValidator transactionValidator;
    private TransactionProcessor transactionProcessor;
    private Blockchain blockchain;
    private ReadonlyUTXOSet utxoSet;
    private Signer signer;
    private BraboConfig config;

    public BlockValidator(@NotNull State state) {
        this.consensus = state.getConsensus();
        this.transactionValidator = state.getTransactionValidator();
        this.transactionProcessor = state.getTransactionProcessor();
        this.blockchain = state.getBlockchain();
        this.utxoSet = state.getChainUTXODatabase();
        this.signer = state.getSigner();
        this.config = state.getConfig();
    }

    private FactMap createFactMap(@NotNull Block block) {
        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        facts.put("transactionValidator", transactionValidator);
        facts.put("transactionProcessor", transactionProcessor);
        facts.put("blockchain", blockchain);
        facts.put("utxoSet", utxoSet);
        facts.put("signer", signer);
        facts.put("config", config);

        return facts;
    }

    public BlockValidationResult checkIncomingBlockValid(@NotNull Block block) {
        return BlockValidationResult.from(new RuleBook(INCOMING_BLOCK).run(createFactMap(block)));
    }

    public BlockValidationResult checkPostOrphanBlockValid(@NotNull Block block) {
        return BlockValidationResult.from(new RuleBook(AFTER_ORPHAN).run(createFactMap(block)));
    }

    public BlockValidationResult checkConnectBlockValid(@NotNull Block block) {
        return BlockValidationResult.from(new RuleBook(CONNECT_TO_CHAIN).run(createFactMap(block)));
    }
}
