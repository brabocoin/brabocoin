package org.brabocoin.brabocoin.validation.block.rules;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.MerkleTree;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.*;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.*;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.processor.BlockProcessor;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.processor.UTXOProcessor;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.util.BigIntegerUtil;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.brabocoin.brabocoin.validation.fact.FactMap;
import org.brabocoin.brabocoin.validation.rule.RuleBook;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockRuleTests {
    static BraboConfig defaultConfig = BraboConfigProvider.getConfig().bind("brabo", BraboConfig.class);
    Consensus consensus = new Consensus();

    private static final ByteString ZERO = ByteString.copyFrom(new byte[]{0});
    private static final EllipticCurve CURVE = EllipticCurve.secp256k1();

    private static Signer signer;


    @BeforeAll
    static void setUp() {
        defaultConfig = new MockBraboConfig(defaultConfig) {
            @Override
            public String blockStoreDirectory() {
                return "src/test/resources/" + super.blockStoreDirectory();
            }

            @Override
            public String utxoStoreDirectory() {
                return "src/test/resources/" + super.utxoStoreDirectory();
            }
        };

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
                Collections.emptyList()
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
                Collections.emptyList()
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
                                )
                        )
                )
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
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(UniqueUnspentCoinbaseBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
        Signer signer = new Signer(EllipticCurve.secp256k1());
        TransactionValidator transactionValidator = new TransactionValidator(
                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
        );
        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
                transactionPool, chainUtxoDatabase, poolUtxo);
        BlockValidator blockValidator = new BlockValidator(
                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
        );
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        blockProcessor.processNewBlock(block);

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", chainUtxoDatabase);

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
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(DuplicateStorageBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);

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
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(DuplicateStorageBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
        Signer signer = new Signer(EllipticCurve.secp256k1());
        TransactionValidator transactionValidator = new TransactionValidator(
                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
        );
        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
                transactionPool, chainUtxoDatabase, poolUtxo);
        BlockValidator blockValidator = new BlockValidator(
                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
        );
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        blockProcessor.processNewBlock(block);

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("blockchain", blockchain);

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
                        new Transaction(
                                Collections.emptyList(),
                                Collections.singletonList(
                                        new Output(Simulation.randomHash(), 10L)
                                )
                        )
                )
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
                                )
                        )
                )
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
                Collections.emptyList()
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
                                )
                        )
                )
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
                        new Transaction(
                                Collections.emptyList(),
                                Collections.singletonList(
                                        new Output(Simulation.randomHash(), 10L)
                                )
                        ),
                        new Transaction(
                                Collections.emptyList(),
                                Collections.singletonList(
                                        new Output(Simulation.randomHash(), 10L)
                                )
                        )
                )
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
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(KnownParentBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);

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
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(KnownParentBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("blockchain", blockchain);

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void LegalTransactionFeesBlkRuleSuccess() throws DatabaseException {
        Transaction cb = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                )
        );
        Block coinbase = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(),
                1,
                Collections.singletonList(
                        cb
                )
        );

        Transaction cb2 = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(
                        new Output(Simulation.randomHash(), 20L)
                )
        );
        Block coinbase2 = new Block(
                coinbase.getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(),
                coinbase.getBlockHeight() + 1,
                Collections.singletonList(
                        cb2
                )
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
                                )
                        ),
                        new Transaction(
                                Arrays.asList(
                                        new Input(Simulation.randomSignature(),
                                                cb.getHash(),
                                                0
                                                ),
                                        new Input(Simulation.randomSignature(),
                                                cb2.getHash(),
                                                0
                                        )
                                ),
                                Collections.singletonList(
                                        new Output(Simulation.randomHash(), 29L)
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(LegalTransactionFeesBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
        Signer signer = new Signer(EllipticCurve.secp256k1());
        TransactionValidator transactionValidator = new TransactionValidator(
                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
        );
        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
                transactionPool, chainUtxoDatabase, poolUtxo);
        BlockValidator blockValidator = new BlockValidator(
                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
        );
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        blockProcessor.processNewBlock(coinbase);
        blockProcessor.processNewBlock(coinbase2);

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", chainUtxoDatabase);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void LegalTransactionFeesBlkRuleFail() throws DatabaseException {
        Transaction cb = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                )
        );
        Block coinbase = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(),
                1,
                Collections.singletonList(
                        cb
                )
        );

        Transaction cb2 = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(
                        new Output(Simulation.randomHash(), 20L)
                )
        );
        Block coinbase2 = new Block(
                coinbase.getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(),
                coinbase.getBlockHeight() + 1,
                Collections.singletonList(
                        cb2
                )
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
                                )
                        ),
                        new Transaction(
                                Arrays.asList(
                                        new Input(Simulation.randomSignature(),
                                                cb.getHash(),
                                                0
                                                ),
                                        new Input(Simulation.randomSignature(),
                                                cb2.getHash(),
                                                0
                                        )
                                ),
                                Collections.singletonList(
                                        new Output(Simulation.randomHash(), 30L)
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(LegalTransactionFeesBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
        Signer signer = new Signer(EllipticCurve.secp256k1());
        TransactionValidator transactionValidator = new TransactionValidator(
                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
        );
        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
                transactionPool, chainUtxoDatabase, poolUtxo);
        BlockValidator blockValidator = new BlockValidator(
                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
        );
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        blockProcessor.processNewBlock(coinbase);
        blockProcessor.processNewBlock(coinbase2);

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", chainUtxoDatabase);

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void LegalTransactionFeesBlkRuleFailOverflow() throws DatabaseException {
        Transaction cb = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(
                        new Output(Simulation.randomHash(), consensus.getMaxMoneyValue() / 2L + 5L)
                )
        );
        Block coinbase = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(),
                1,
                Collections.singletonList(
                        cb
                )
        );

        Transaction cb2 = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(
                        new Output(Simulation.randomHash(), consensus.getMaxMoneyValue() / 2L + 5L)
                )
        );
        Block coinbase2 = new Block(
                coinbase.getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(),
                coinbase.getBlockHeight() + 1,
                Collections.singletonList(
                        cb2
                )
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
                                )
                        ),
                        new Transaction(
                                Arrays.asList(
                                        new Input(Simulation.randomSignature(),
                                                cb.getHash(),
                                                0
                                                ),
                                        new Input(Simulation.randomSignature(),
                                                cb2.getHash(),
                                                0
                                        )
                                ),
                                Collections.singletonList(
                                        new Output(Simulation.randomHash(), 1L)
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(LegalTransactionFeesBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
        Signer signer = new Signer(EllipticCurve.secp256k1());
        TransactionValidator transactionValidator = new TransactionValidator(
                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
        );
        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
                transactionPool, chainUtxoDatabase, poolUtxo);
        BlockValidator blockValidator = new BlockValidator(
                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
        );
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        blockProcessor.processNewBlock(coinbase);
        blockProcessor.processNewBlock(coinbase2);

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", chainUtxoDatabase);
        
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
                                )
                        )
                )
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
                                )
                        )
                )
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
                                )
                        )
                )
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
                Simulation.repeatedBuilder(() -> Simulation.randomTransaction(1, 20000), 4)
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
                                )
                        )
                )
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
                Collections.emptyList()
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
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(SatisfiesTargetValueBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Disabled // TODO: Hash comparison van Sten moet ff erin
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
                                )
                        )
                )
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
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidBlockHeightBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);

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
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidBlockHeightBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);

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
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidCoinbaseOutputAmountBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
        Signer signer = new Signer(EllipticCurve.secp256k1());
        TransactionValidator transactionValidator = new TransactionValidator(
                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
        );
        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
                transactionPool, chainUtxoDatabase, poolUtxo);
        BlockValidator blockValidator = new BlockValidator(
                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
        );

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", chainUtxoDatabase);

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
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidCoinbaseOutputAmountBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
        Signer signer = new Signer(EllipticCurve.secp256k1());
        TransactionValidator transactionValidator = new TransactionValidator(
                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
        );
        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
                transactionPool, chainUtxoDatabase, poolUtxo);
        BlockValidator blockValidator = new BlockValidator(
                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
        );

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", chainUtxoDatabase);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidCoinbaseOutputAmountBlkRuleSuccessTransactionsEdge() throws DatabaseException {
        Transaction cb = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                )
        );
        Block coinbase = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(),
                1,
                Collections.singletonList(
                        cb
                )
        );

        Transaction cb2 = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(
                        new Output(Simulation.randomHash(), 20L)
                )
        );
        Block coinbase2 = new Block(
                coinbase.getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(),
                coinbase.getBlockHeight() + 1,
                Collections.singletonList(
                        cb2
                )
        );

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
        Signer signer = new Signer(EllipticCurve.secp256k1());
        TransactionValidator transactionValidator = new TransactionValidator(
                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
        );
        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
                transactionPool, chainUtxoDatabase, poolUtxo);
        BlockValidator blockValidator = new BlockValidator(
                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
        );
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        blockProcessor.processNewBlock(coinbase);
        blockProcessor.processNewBlock(coinbase2);

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
                                        new Output(Simulation.randomHash(),
                                                consensus.getBlockReward()
                                                + 5L
                                        )
                                )
                        ),
                        new Transaction(
                                Collections.singletonList(
                                        new Input(Simulation.randomSignature(),
                                                cb.getHash(),
                                                0
                                                )
                                ),
                                Collections.singletonList(
                                        new Output(Simulation.randomHash(),
                                                8)
                                )
                        ),
                        new Transaction(
                                Collections.singletonList(
                                        new Input(Simulation.randomSignature(),
                                                cb2.getHash(),
                                                0
                                        )
                                ),
                                Collections.singletonList(
                                        new Output(Simulation.randomHash(),
                                                17)
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidCoinbaseOutputAmountBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", chainUtxoDatabase);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidCoinbaseOutputAmountBlkRuleFailTransactionsEdge() throws DatabaseException {
        Transaction cb = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(
                        new Output(Simulation.randomHash(), 10L)
                )
        );
        Block coinbase = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(),
                1,
                Collections.singletonList(
                        cb
                )
        );

        Transaction cb2 = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(
                        new Output(Simulation.randomHash(), 20L)
                )
        );
        Block coinbase2 = new Block(
                coinbase.getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(),
                coinbase.getBlockHeight() + 1,
                Collections.singletonList(
                        cb2
                )
        );

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
        Signer signer = new Signer(EllipticCurve.secp256k1());
        TransactionValidator transactionValidator = new TransactionValidator(
                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
        );
        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
                transactionPool, chainUtxoDatabase, poolUtxo);
        BlockValidator blockValidator = new BlockValidator(
                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
        );
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        blockProcessor.processNewBlock(coinbase);
        blockProcessor.processNewBlock(coinbase2);

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
                                        new Output(Simulation.randomHash(),
                                                consensus.getBlockReward()
                                                + 6L
                                        )
                                )
                        ),
                        new Transaction(
                                Collections.singletonList(
                                        new Input(Simulation.randomSignature(),
                                                cb.getHash(),
                                                0
                                                )
                                ),
                                Collections.singletonList(
                                        new Output(Simulation.randomHash(),
                                                8)
                                )
                        ),
                        new Transaction(
                                Collections.singletonList(
                                        new Input(Simulation.randomSignature(),
                                                cb2.getHash(),
                                                0
                                        )
                                ),
                                Collections.singletonList(
                                        new Output(Simulation.randomHash(),
                                                17)
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidCoinbaseOutputAmountBlkRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", chainUtxoDatabase);

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
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidCoinbaseOutputAmountBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
        Signer signer = new Signer(EllipticCurve.secp256k1());
        TransactionValidator transactionValidator = new TransactionValidator(
                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
        );
        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
                transactionPool, chainUtxoDatabase, poolUtxo);
        BlockValidator blockValidator = new BlockValidator(
                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
        );

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("utxoSet", chainUtxoDatabase);

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidMerkleRootBlkRuleSuccess() {
        List<Transaction> transactionList = Simulation.repeatedBuilder(() -> Simulation.randomTransaction(10, 10), 100);

        Hash merkleRoot = new MerkleTree(consensus.getMerkleTreeHashFunction(), transactionList.stream().map(Transaction::getHash).collect(Collectors.toList())).getRoot();

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                merkleRoot,
                Simulation.randomHash(),
                Simulation.randomBigInteger(),
                1,
                transactionList
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
        List<Transaction> transactionList = Simulation.repeatedBuilder(() -> Simulation.randomTransaction(10, 10), 100);

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomBigInteger(),
                1,
                transactionList
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
                                )
                        )
                )
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
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidParentBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
        Signer signer = new Signer(EllipticCurve.secp256k1());
        TransactionValidator transactionValidator = new TransactionValidator(
                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
        );
        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
                transactionPool, chainUtxoDatabase, poolUtxo);
        BlockValidator blockValidator = new BlockValidator(
                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
        );
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        blockProcessor.processNewBlock(parent);

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("blockchain", blockchain);

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
                                )
                        )
                )
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
                                )
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidParentBlkRule.class)
        ));

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        UTXODatabase poolUtxo = new UTXODatabase(new HashMapDB());
        Signer signer = new Signer(EllipticCurve.secp256k1());
        TransactionValidator transactionValidator = new TransactionValidator(
                consensus, blockchain.getMainChain(), transactionPool, chainUtxoDatabase, poolUtxo, signer
        );
        TransactionProcessor transactionProcessor = new TransactionProcessor(transactionValidator,
                transactionPool, chainUtxoDatabase, poolUtxo);
        BlockValidator blockValidator = new BlockValidator(
                consensus, transactionValidator, transactionProcessor, blockchain, chainUtxoDatabase, signer
        );
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        blockProcessor.processNewBlock(parent);
        blockchain.setBlockInvalid(parent.getHash());

        FactMap facts = new FactMap();
        facts.put("block", block);
        facts.put("consensus", consensus);
        facts.put("blockchain", blockchain);

        assertFalse(ruleBook.run(facts).isPassed());
    }
}
