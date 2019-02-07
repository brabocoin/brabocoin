package org.brabocoin.brabocoin.validation.block.rules;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.MerkleTree;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.testutil.TestState;
import org.brabocoin.brabocoin.util.BigIntegerUtil;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.block.BlockValidationResult;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.brabocoin.brabocoin.validation.fact.FactMap;
import org.brabocoin.brabocoin.validation.rule.RuleBook;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockRuleTests {

    static BraboConfig defaultConfig = BraboConfigProvider.getConfig()
        .bind("brabo", BraboConfig.class);
    Consensus consensus = new Consensus();

    private static final ByteString ZERO = ByteString.copyFrom(new byte[] {0});
    private static final EllipticCurve CURVE = EllipticCurve.secp256k1();

    private static Signer signer;

    @BeforeAll
    static void setUp() {
        defaultConfig = new MockBraboConfig(defaultConfig);

        signer = new Signer(CURVE);
    }

    @Test
    void CorrectTargetValueBlkRuleSuccess() {
        Block block = new Block(
            Simulation.randomHash(),
            Simulation.randomHash(),
            consensus.getTargetValue(),
            Simulation.randomBigInteger(),
            0,
            Collections.emptyList(),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(CorrectTargetValueBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void CorrectTargetValueBlkRuleFail() {
        Block block = new Block(
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            0,
            Collections.emptyList(),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(CorrectTargetValueBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void DuplicateCoinbaseBlkRuleSuccess() throws DatabaseException {
        Block block = new Block(
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            0,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(UniqueUnspentCoinbaseBlkRule.class)
        ));

        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", chainUtxoDatabase);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void DuplicateCoinbaseBlkRuleFail() throws DatabaseException {
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(UniqueUnspentCoinbaseBlkRule.class)
        ));

        State state = new TestState(defaultConfig) {
            @Override
            protected BlockValidator createBlockValidator() {
                return new BlockValidator(
                    this
                ) {
                    @Override
                    public BlockValidationResult validate(@NotNull Block block,
                                                          @NotNull RuleList ruleList) {
                        return BlockValidationResult.passed();
                    }
                };
            }
        };

        state.getBlockProcessor().processNewBlock(block, false);

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", state.getChainUTXODatabase());

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void DuplicateStorageBlkRuleSuccess() throws DatabaseException {
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(DuplicateStorageBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(
            new HashMapDB(),
            new File(defaultConfig.blockStoreDirectory()),
            defaultConfig.maxBlockFileSize()
        );
        Blockchain blockchain = new Blockchain(blockDatabase, consensus, 100, 0, new Random());

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("blockchain", blockchain);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void DuplicateStorageBlkRuleFail() throws DatabaseException {
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(DuplicateStorageBlkRule.class)
        ));

        State state = new TestState(defaultConfig) {
            @Override
            protected BlockValidator createBlockValidator() {
                return new BlockValidator(
                    this
                ) {
                    @Override
                    public BlockValidationResult validate(@NotNull Block block,
                                                          @NotNull RuleList ruleList) {
                        return BlockValidationResult.passed();
                    }
                };
            }
        };

        state.getBlockProcessor().processNewBlock(block, false);

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("blockchain", state.getBlockchain());

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void HasCoinbaseBlkRuleSuccess() {
        Block block = new Block(
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            0,
            Collections.singletonList(
                Transaction.coinbase(new Output(Simulation.randomHash(), 10L), 0)
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(HasCoinbaseBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void HasCoinbaseBlkRuleFail() {
        Block block = new Block(
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            0,
            Collections.singletonList(
                new Transaction(
                    Collections.singletonList(
                        Simulation.randomInput()
                    ),
                    Collections.singletonList(
                        Simulation.randomOutput()
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(HasCoinbaseBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void HasCoinbaseBlkRuleFailEmpty() {
        Block block = new Block(
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            0,
            Collections.emptyList(),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(HasCoinbaseBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void HasSingleCoinbaseBlkRuleSuccess() {
        Block block = new Block(
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            0,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(HasSingleCoinbaseBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void HasSingleCoinbaseBlkRuleFail() {
        Block block = new Block(
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            0,
            Arrays.asList(
                Transaction.coinbase(new Output(Simulation.randomHash(), 10L), 0),
                Transaction.coinbase(new Output(Simulation.randomHash(), 10L), 0)
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(HasSingleCoinbaseBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void KnownParentBlkRuleSuccess() throws DatabaseException {
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(KnownParentBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(
            new HashMapDB(),
            new File(defaultConfig.blockStoreDirectory()),
            defaultConfig.maxBlockFileSize()
        );
        Blockchain blockchain = new Blockchain(blockDatabase, consensus, 100, 0, new Random());

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("blockchain", blockchain);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void KnownParentBlkRuleFail() throws DatabaseException {
        Block block = new Block(
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(KnownParentBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(
            new HashMapDB(),
            new File(defaultConfig.blockStoreDirectory()),
            defaultConfig.maxBlockFileSize()
        );
        Blockchain blockchain = new Blockchain(blockDatabase, consensus, 100, 0, new Random());

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("blockchain", blockchain);

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void LegalTransactionFeesBlkRuleSuccess() throws DatabaseException {
        Transaction cb = Transaction.coinbase(new Output(Simulation.randomHash(), 10L), 1);
        Block coinbase = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                cb
            ),
            0
        );

        Transaction cb2 = Transaction.coinbase(new Output(Simulation.randomHash(), 20L), 2);
        Block coinbase2 = new Block(
            coinbase.getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            coinbase.getBlockHeight() + 1,
            Collections.singletonList(
                cb2
            ),
            0
        );

        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Arrays.asList(
                Transaction.coinbase(new Output(Simulation.randomHash(), 10L), 1),
                new Transaction(
                    Arrays.asList(
                        new Input(
                            cb.getHash(),
                            0
                        ),
                        new Input(
                            cb2.getHash(),
                            0
                        )
                    ),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 29L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(LegalTransactionFeesBlkRule.class)
        ));

        State state = new TestState(defaultConfig) {
            @Override
            protected BlockValidator createBlockValidator() {
                return new BlockValidator(
                    this
                ) {
                    @Override
                    public BlockValidationResult validate(@NotNull Block block,
                                                          @NotNull RuleList ruleList) {
                        return BlockValidationResult.passed();
                    }
                };
            }
        };

        state.getBlockProcessor().processNewBlock(coinbase, false);
        state.getBlockProcessor().processNewBlock(coinbase2, false);

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", state.getChainUTXODatabase());

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void LegalTransactionFeesBlkRuleFail() throws DatabaseException {
        Transaction cb = new Transaction(
            Collections.emptyList(),
            Collections.singletonList(
                new Output(Simulation.randomHash(), 10L)
            ), Collections.emptyList()
        );
        Block coinbase = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                cb
            ),
            0
        );

        Transaction cb2 = new Transaction(
            Collections.emptyList(),
            Collections.singletonList(
                new Output(Simulation.randomHash(), 20L)
            ), Collections.emptyList()
        );
        Block coinbase2 = new Block(
            coinbase.getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            coinbase.getBlockHeight() + 1,
            Collections.singletonList(
                cb2
            ),
            0
        );

        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Arrays.asList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                ),
                new Transaction(
                    Arrays.asList(
                        new Input(
                            cb.getHash(),
                            0
                        ),
                        new Input(
                            cb2.getHash(),
                            0
                        )
                    ),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 30L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(LegalTransactionFeesBlkRule.class)
        ));
        State state = new TestState(defaultConfig);

        state.getBlockProcessor().processNewBlock(coinbase, false);
        state.getBlockProcessor().processNewBlock(coinbase2, false);

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", state.getChainUTXODatabase());

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void LegalTransactionFeesBlkRuleFailOverflow() throws DatabaseException {
        Transaction cb = new Transaction(
            Collections.emptyList(),
            Collections.singletonList(
                new Output(Simulation.randomHash(), consensus.getMaxMoneyValue() / 2L + 5L)
            ), Collections.emptyList()
        );
        Block coinbase = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                cb
            ),
            0
        );

        Transaction cb2 = new Transaction(
            Collections.emptyList(),
            Collections.singletonList(
                new Output(Simulation.randomHash(), consensus.getMaxMoneyValue() / 2L + 5L)
            ), Collections.emptyList()
        );
        Block coinbase2 = new Block(
            coinbase.getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            coinbase.getBlockHeight() + 1,
            Collections.singletonList(
                cb2
            ),
            0
        );

        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Arrays.asList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                ),
                new Transaction(
                    Arrays.asList(
                        new Input(
                            cb.getHash(),
                            0
                        ),
                        new Input(
                            cb2.getHash(),
                            0
                        )
                    ),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 1L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(LegalTransactionFeesBlkRule.class)
        ));
        State state = new TestState(defaultConfig);

        state.getBlockProcessor().processNewBlock(coinbase, false);
        state.getBlockProcessor().processNewBlock(coinbase2, false);

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", state.getChainUTXODatabase());

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void MaxNonceBlkRuleSuccess() throws DatabaseException {
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(MaxNonceBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void MaxNonceBlkRuleFail() throws DatabaseException {
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            consensus.getMaxNonce().add(BigInteger.ONE),
            1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(MaxNonceBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void MaxSizeBlkRuleSuccess() {
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(MaxSizeBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void MaxSizeBlkRuleFail() {
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Simulation.repeatedBuilder(() -> Simulation.randomTransaction(1, 20000), 4),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(MaxSizeBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void NonEmptyTransactionListBlkRuleSuccess() {
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(NonEmptyTransactionListBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void NonEmptyTransactionListBlkRuleFail() {
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.emptyList(),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(NonEmptyTransactionListBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void SatisfiesTargetValueBlkRuleSuccess() {
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            new Hash(ByteString.copyFrom(BigIntegerUtil.getMaxBigInteger(33).toByteArray())),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(SatisfiesTargetValueBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void SatisfiesTargetValueBlkRuleFail() {
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            new Hash(ByteString.copyFrom(BigIntegerUtil.getMaxBigInteger(1).toByteArray())),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(SatisfiesTargetValueBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidBlockHeightBlkRuleSuccess() throws DatabaseException {
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(ValidBlockHeightBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(
            new HashMapDB(),
            new File(defaultConfig.blockStoreDirectory()),
            defaultConfig.maxBlockFileSize()
        );
        Blockchain blockchain = new Blockchain(blockDatabase, consensus, 100, 0, new Random());

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("blockchain", blockchain);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidBlockHeightBlkRuleFail() throws DatabaseException {
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            2,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(ValidBlockHeightBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(
            new HashMapDB(),
            new File(defaultConfig.blockStoreDirectory()),
            defaultConfig.maxBlockFileSize()
        );
        Blockchain blockchain = new Blockchain(blockDatabase, consensus, 100, 0, new Random());

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("blockchain", blockchain);

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidCoinbaseOutputAmountBlkRuleSuccess() throws DatabaseException {
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(ValidCoinbaseOutputAmountBlkRule.class)
        ));
        State state = new TestState(defaultConfig);

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", state.getChainUTXODatabase());

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidCoinbaseOutputAmountBlkRuleSuccessEdge() throws DatabaseException {
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), consensus.getBlockReward())
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(ValidCoinbaseOutputAmountBlkRule.class)
        ));
        State state = new TestState(defaultConfig);

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", state.getChainUTXODatabase());

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidCoinbaseOutputAmountBlkRuleSuccessTransactionsEdge() throws DatabaseException {
        Transaction cb = new Transaction(
            Collections.emptyList(),
            Collections.singletonList(
                new Output(Simulation.randomHash(), 10L)
            ), Collections.emptyList()
        );
        Block coinbase = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                cb
            ),
            0
        );

        Transaction cb2 = new Transaction(
            Collections.emptyList(),
            Collections.singletonList(
                new Output(Simulation.randomHash(), 20L)
            ), Collections.emptyList()
        );
        Block coinbase2 = new Block(
            coinbase.getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            coinbase.getBlockHeight() + 1,
            Collections.singletonList(
                cb2
            ),
            0
        );

        State state = new TestState(defaultConfig) {
            @Override
            protected BlockValidator createBlockValidator() {
                return new BlockValidator(
                    this
                ) {
                    @Override
                    public BlockValidationResult validate(@NotNull Block block,
                                                          @NotNull RuleList ruleList) {
                        return BlockValidationResult.passed();
                    }
                };
            }
        };

        state.getBlockProcessor().processNewBlock(coinbase, false);
        state.getBlockProcessor().processNewBlock(coinbase2, false);

        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Arrays.asList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(
                            Simulation.randomHash(),
                            consensus.getBlockReward()
                                + 5L
                        )
                    ), Collections.emptyList()
                ),
                new Transaction(
                    Collections.singletonList(
                        new Input(
                            cb.getHash(),
                            0
                        )
                    ),
                    Collections.singletonList(
                        new Output(
                            Simulation.randomHash(),
                            8
                        )
                    ), Collections.emptyList()
                ),
                new Transaction(
                    Collections.singletonList(
                        new Input(
                            cb2.getHash(),
                            0
                        )
                    ),
                    Collections.singletonList(
                        new Output(
                            Simulation.randomHash(),
                            17
                        )
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(ValidCoinbaseOutputAmountBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", state.getChainUTXODatabase());

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidCoinbaseOutputAmountBlkRuleFailTransactionsEdge() throws DatabaseException {
        Transaction cb = new Transaction(
            Collections.emptyList(),
            Collections.singletonList(
                new Output(Simulation.randomHash(), 10L)
            ), Collections.emptyList()
        );
        Block coinbase = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                cb
            ),
            0
        );

        Transaction cb2 = new Transaction(
            Collections.emptyList(),
            Collections.singletonList(
                new Output(Simulation.randomHash(), 20L)
            ), Collections.emptyList()
        );
        Block coinbase2 = new Block(
            coinbase.getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            coinbase.getBlockHeight() + 1,
            Collections.singletonList(
                cb2
            ),
            0
        );
        State state = new TestState(defaultConfig);

        state.getBlockProcessor().processNewBlock(coinbase, false);
        state.getBlockProcessor().processNewBlock(coinbase2, false);

        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Arrays.asList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(
                            Simulation.randomHash(),
                            consensus.getBlockReward()
                                + 6L
                        )
                    ), Collections.emptyList()
                ),
                new Transaction(
                    Collections.singletonList(
                        new Input(
                            cb.getHash(),
                            0
                        )
                    ),
                    Collections.singletonList(
                        new Output(
                            Simulation.randomHash(),
                            8
                        )
                    ), Collections.emptyList()
                ),
                new Transaction(
                    Collections.singletonList(
                        new Input(
                            cb2.getHash(),
                            0
                        )
                    ),
                    Collections.singletonList(
                        new Output(
                            Simulation.randomHash(),
                            17
                        )
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(ValidCoinbaseOutputAmountBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", state.getChainUTXODatabase());

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidCoinbaseOutputAmountBlkRuleFailEdge() throws DatabaseException {
        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), consensus.getBlockReward() + 1)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(ValidCoinbaseOutputAmountBlkRule.class)
        ));
        State state = new TestState(defaultConfig);

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", state.getChainUTXODatabase());

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidMerkleRootBlkRuleSuccess() {
        List<Transaction> transactionList =
            Simulation.repeatedBuilder(() -> Simulation.randomTransaction(
                10,
                10
            ), 100);

        Hash merkleRoot = new MerkleTree(
            consensus.getMerkleTreeHashFunction(),
            transactionList.stream().map(Transaction::getHash).collect(Collectors.toList())
        ).getRoot();

        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            merkleRoot,
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            transactionList,
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(ValidMerkleRootBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidMerkleRootBlkRuleFail() {
        List<Transaction> transactionList =
            Simulation.repeatedBuilder(() -> Simulation.randomTransaction(
                10,
                10
            ), 100);

        Block block = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            transactionList,
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(ValidMerkleRootBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidParentBlkRuleSuccess() throws DatabaseException {
        Block parent = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        Block block = new Block(
            parent.getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            parent.getBlockHeight() + 1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(ValidParentBlkRule.class)
        ));
        State state = new TestState(defaultConfig) {
            @Override
            protected BlockValidator createBlockValidator() {
                return new BlockValidator(
                    this
                ) {
                    @Override
                    public BlockValidationResult validate(@NotNull Block block,
                                                          @NotNull RuleList ruleList) {
                        return BlockValidationResult.passed();
                    }
                };
            }
        };

        state.getBlockProcessor().processNewBlock(parent, false);

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("blockchain", state.getBlockchain());

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidParentBlkRuleFail() throws DatabaseException {
        Block parent = new Block(
            consensus.getGenesisBlock().getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        Block block = new Block(
            parent.getHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            parent.getBlockHeight() + 1,
            Collections.singletonList(
                new Transaction(
                    Collections.emptyList(),
                    Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                    ), Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(ValidParentBlkRule.class)
        ));
        State state = new TestState(defaultConfig) {
            @Override
            protected BlockValidator createBlockValidator() {
                return new BlockValidator(
                    this
                ) {
                    @Override
                    public BlockValidationResult validate(@NotNull Block block,
                                                          @NotNull RuleList ruleList) {
                        return BlockValidationResult.passed();
                    }
                };
            }
        };

        state.getBlockProcessor().processNewBlock(parent, false);
        state.getBlockchain().setBlockInvalid(parent.getHash());

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("blockchain", state.getBlockchain());

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidNetworkIdBlkRuleSuccess() {
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public int networkId() {
                return 666;
            }
        };

        Block block = new Block(
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            0,
            Collections.emptyList(),
            666
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(ValidNetworkIdBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("config", config);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidNetworkIdBlkRuleFailed() {
        BraboConfig config = new MockBraboConfig(defaultConfig) {
            @Override
            public int networkId() {
                return 667;
            }
        };

        Block block = new Block(
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            0,
            Collections.emptyList(),
            666
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(ValidNetworkIdBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("config", config);

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void DuplicateInputBlkRuleSuccess() {
        Block block = new Block(
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            0,
            Arrays.asList(
                new Transaction(
                    Collections.singletonList(new Input(new Hash(ByteString.copyFromUtf8("a")), 0)),
                    Collections.emptyList(),
                    Collections.emptyList()
                ),
                new Transaction(
                    Collections.singletonList(new Input(new Hash(ByteString.copyFromUtf8("a")), 1)),
                    Collections.emptyList(),
                    Collections.emptyList()
                ),
                new Transaction(
                    Collections.singletonList(new Input(new Hash(ByteString.copyFromUtf8("b")), 0)),
                    Collections.emptyList(),
                    Collections.emptyList()
                ),
                new Transaction(
                    Collections.singletonList(new Input(new Hash(ByteString.copyFromUtf8("b")), 1)),
                    Collections.emptyList(),
                    Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(DuplicateInputBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void DuplicateInputBlkRuleFail() {
        Block block = new Block(
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            0,
            Arrays.asList(
                new Transaction(
                    Collections.singletonList(new Input(new Hash(ByteString.copyFromUtf8("a")), 0)),
                    Collections.emptyList(),
                    Collections.emptyList()
                ),
                new Transaction(
                    Collections.singletonList(new Input(new Hash(ByteString.copyFromUtf8("a")), 1)),
                    Collections.emptyList(),
                    Collections.emptyList()
                ),
                new Transaction(
                    Collections.singletonList(new Input(new Hash(ByteString.copyFromUtf8("a")), 0)),
                    Collections.emptyList(),
                    Collections.emptyList()
                ),
                new Transaction(
                    Collections.singletonList(new Input(new Hash(ByteString.copyFromUtf8("b")), 1)),
                    Collections.emptyList(),
                    Collections.emptyList()
                )
            ),
            0
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(DuplicateInputBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertFalse(ruleBook.run(facts).isPassed());
    }
}
