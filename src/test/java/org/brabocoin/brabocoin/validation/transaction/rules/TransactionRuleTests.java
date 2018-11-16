package org.brabocoin.brabocoin.validation.transaction.rules;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.PublicKey;
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
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.fact.FactMap;
import org.brabocoin.brabocoin.validation.rule.RuleBook;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionRuleTests {
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
    void CoinbaseCreationTxRuleFail() {
        Transaction transaction = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(Simulation.randomOutput())
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(CoinbaseCreationTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void CoinbaseCreationTxRuleSuccess() {
        Transaction transaction = new Transaction(
                Collections.singletonList(Simulation.randomInput()),
                Collections.singletonList(Simulation.randomOutput())
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(CoinbaseCreationTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void CoinbaseMaturityTxRuleFail() throws DatabaseException {
        Output output = Simulation.randomOutput();
        Transaction coinbase = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(output)
        );

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(coinbase)
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

        blockProcessor.processNewBlock(block);

        Transaction spendCoinbase = new Transaction(
                Collections.singletonList(new Input(Simulation.randomSignature(), coinbase.getHash(), 0)),
                Collections.singletonList(Simulation.randomOutput())
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(CoinbaseMaturityTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("mainChain", blockchain.getMainChain());
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));
        facts.put("transaction", spendCoinbase);
        facts.put("consensus", consensus);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void CoinbaseMaturityTxRuleSuccess() throws DatabaseException {
        Output output = Simulation.randomOutput();
        Transaction coinbase = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(output)
        );

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(coinbase)
        );
        List<Block> blockList = new ArrayList<Block>() {{
            add(block);
        }};
        blockList.addAll(Simulation.randomBlockChainGenerator(100, block.getHash(), 2, 0, 5));

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

        for (Block b : blockList) {
            blockProcessor.processNewBlock(b);
        }

        Transaction spendCoinbase = new Transaction(
                Collections.singletonList(new Input(Simulation.randomSignature(), coinbase.getHash(), 0)),
                Collections.singletonList(Simulation.randomOutput())
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(CoinbaseMaturityTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("mainChain", blockchain.getMainChain());
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));
        facts.put("transaction", spendCoinbase);
        facts.put("consensus", consensus);

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void DuplicateInputTxRuleFail() {
        Input input = Simulation.randomInput();
        Transaction transaction = new Transaction(
                Arrays.asList(input, input),
                Collections.singletonList(Simulation.randomOutput())
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(DuplicateInputTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void DuplicateInputTxRuleSuccess() {
        Input input = Simulation.randomInput();
        Transaction transaction = new Transaction(
                Simulation.repeatedBuilder(Simulation::randomInput, 100),
                Collections.singletonList(Simulation.randomOutput())
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(DuplicateInputTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void DuplicatePoolTxRuleFailIndependent() {
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());


        Input input = Simulation.randomInput();
        Transaction transaction = new Transaction(
                Arrays.asList(input, input),
                Collections.singletonList(Simulation.randomOutput())
        );

        transactionPool.addIndependentTransaction(transaction);

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(DuplicatePoolTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);
        facts.put("transactionPool", transactionPool);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void DuplicatePoolTxRuleFailDependent() {
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());


        Input input = Simulation.randomInput();
        Transaction transaction = new Transaction(
                Arrays.asList(input, input),
                Collections.singletonList(Simulation.randomOutput())
        );

        transactionPool.addDependentTransaction(transaction);

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(DuplicatePoolTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);
        facts.put("transactionPool", transactionPool);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void DuplicatePoolTxRuleFailOrphan() {
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());


        Input input = Simulation.randomInput();
        Transaction transaction = new Transaction(
                Arrays.asList(input, input),
                Collections.singletonList(Simulation.randomOutput())
        );

        transactionPool.addOrphanTransaction(transaction);

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(DuplicatePoolTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);
        facts.put("transactionPool", transactionPool);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void DuplicatePoolTxRuleSuccess() {
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());

        Input input = Simulation.randomInput();
        Transaction transaction = new Transaction(
                Arrays.asList(input, input),
                Collections.singletonList(Simulation.randomOutput())
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(DuplicatePoolTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);
        facts.put("transactionPool", transactionPool);

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void OutputCountTxRuleCoinbaseFail() {
        Transaction transaction = new Transaction(
                Collections.emptyList(),
                Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputOutputNotEmptyTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void OutputCountTxRuleCoinbaseSuccess() {
        Transaction transaction = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(Simulation.randomOutput())
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputOutputNotEmptyTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void OutputCountTxRuleNormalFailInputs() {
        Transaction transaction = new Transaction(
                Collections.emptyList(),
                Arrays.asList(Simulation.randomOutput(), Simulation.randomOutput())
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputOutputNotEmptyTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void OutputCountTxRuleNormalFailOutputs() {
        Transaction transaction = new Transaction(
                Collections.singletonList(Simulation.randomInput()),
                Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputOutputNotEmptyTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void OutputCountTxRuleNormalSuccess() {
        Transaction transaction = new Transaction(
                Collections.singletonList(Simulation.randomInput()),
                Arrays.asList(Simulation.randomOutput(), Simulation.randomOutput())
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputOutputNotEmptyTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void InputValueTxRangeFailNegativeBlockProcessor() throws DatabaseException {
        Transaction negativeOutputTransaction = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(new Output(
                        Simulation.randomHash(),
                        -1
                ))
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

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(negativeOutputTransaction)
        );

        blockProcessor.processNewBlock(block);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        Simulation.randomSignature(),
                        negativeOutputTransaction.getHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput())
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputValueTxRange.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTransaction);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void InputValueTxRangeFailNegativeTransactionProcessor() throws DatabaseException {
        Transaction negativeOutputTransaction = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(new Output(
                        Simulation.randomHash(),
                        -1
                ))
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

        transactionProcessor.processNewTransaction(negativeOutputTransaction);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        Simulation.randomSignature(),
                        negativeOutputTransaction.getHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput())
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputValueTxRange.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTransaction);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void InputValueTxRangeFailExceedRangeSingleOutput() throws DatabaseException {
        Transaction negativeOutputTransaction = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(new Output(
                        Simulation.randomHash(),
                        Long.MAX_VALUE - 1
                ))
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

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(negativeOutputTransaction)
        );

        blockProcessor.processNewBlock(block);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        Simulation.randomSignature(),
                        negativeOutputTransaction.getHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput())
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputValueTxRange.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTransaction);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void InputValueTxRangeFailExceedRangeSum() throws DatabaseException {
        Transaction negativeOutputTransaction = new Transaction(
                Collections.emptyList(),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                consensus.getMaxMoneyValue() / 3L
                        ), new Output(
                                Simulation.randomHash(),
                                consensus.getMaxMoneyValue() / 3L
                        ), new Output(
                                Simulation.randomHash(),
                                consensus.getMaxMoneyValue() / 3L + 5
                        )
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

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(negativeOutputTransaction)
        );

        blockProcessor.processNewBlock(block);

        Transaction spendingTransaction = new Transaction(
                Arrays.asList(new Input(
                                Simulation.randomSignature(),
                                negativeOutputTransaction.getHash(),
                                0
                        ),
                        new Input(
                                Simulation.randomSignature(),
                                negativeOutputTransaction.getHash(),
                                1
                        ),
                        new Input(
                                Simulation.randomSignature(),
                                negativeOutputTransaction.getHash(),
                                2
                        )),
                Collections.singletonList(Simulation.randomOutput())
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputValueTxRange.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTransaction);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void InputValueTxRangeFailZeroOutput() throws DatabaseException {
        Transaction zeroOutputTransaction = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(new Output(
                        Simulation.randomHash(),
                        0
                ))
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

        transactionProcessor.processNewTransaction(zeroOutputTransaction);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        Simulation.randomSignature(),
                        zeroOutputTransaction.getHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput())
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputValueTxRange.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTransaction);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void InputValueTxRangeSuccessBlockProcessor() throws DatabaseException {
        Transaction positiveOutputTransaction = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(new Output(
                        Simulation.randomHash(),
                        1
                ))
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

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(positiveOutputTransaction)
        );

        blockProcessor.processNewBlock(block);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        Simulation.randomSignature(),
                        positiveOutputTransaction.getHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput())
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputValueTxRange.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTransaction);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void InputValueTxRangeSuccessTransactionProcessor() throws DatabaseException {
        Transaction positiveOutputTransaction = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(new Output(
                        Simulation.randomHash(),
                        1
                ))
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
        transactionProcessor.processNewTransaction(positiveOutputTransaction);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        Simulation.randomSignature(),
                        positiveOutputTransaction.getHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput())
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputValueTxRange.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTransaction);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void MaxSizeTxRuleFail() {
        Transaction transaction = new Transaction(
                Simulation.repeatedBuilder(Simulation::randomInput, 10000),
                Simulation.repeatedBuilder(Simulation::randomOutput, 10000)
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(MaxSizeTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void MaxSizeTxRuleSuccess() {
        Transaction transaction = new Transaction(
                Simulation.repeatedBuilder(Simulation::randomInput, 5),
                Simulation.repeatedBuilder(Simulation::randomOutput, 5)
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(MaxSizeTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void OutputValueTxRuleFailNegative() {
        Transaction transaction = new Transaction(
                Collections.singletonList(Simulation.randomInput()),
                Collections.singletonList(new Output(Simulation.randomHash(), -1))
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(OutputValueTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void OutputValueTxRuleFailZero() {
        Transaction transaction = new Transaction(
                Collections.singletonList(Simulation.randomInput()),
                Collections.singletonList(new Output(Simulation.randomHash(), 0))
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(OutputValueTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void OutputValueTxRuleFailExceedSingle() {
        Transaction transaction = new Transaction(
                Collections.singletonList(Simulation.randomInput()),
                Collections.singletonList(new Output(Simulation.randomHash(), consensus.getMaxMoneyValue() + 1))
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(OutputValueTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void OutputValueTxRuleFailExceedSum() {
        Transaction transaction = new Transaction(
                Collections.singletonList(Simulation.randomInput()),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                consensus.getMaxMoneyValue() / 3L
                        ), new Output(
                                Simulation.randomHash(),
                                consensus.getMaxMoneyValue() / 3L
                        ), new Output(
                                Simulation.randomHash(),
                                consensus.getMaxMoneyValue() / 3L + 5
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(OutputValueTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void OutputValueTxRuleSuccess() {
        Transaction transaction = new Transaction(
                Collections.singletonList(Simulation.randomInput()),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                consensus.getMaxMoneyValue() / 3L
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(OutputValueTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", transaction);
        facts.put("consensus", consensus);

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void PoolDoubleSpendingTxRuleFail() throws DatabaseException {
        Input input = new Input(
                Simulation.randomSignature(),
                Simulation.randomHash(),
                0
        );
        Transaction spendingTx = new Transaction(
                Collections.singletonList(input),
                Collections.singletonList(
                        new Output(
                                Simulation.randomHash(),
                                20L
                        )
                )
        );

        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        transactionPool.addIndependentTransaction(spendingTx);

        Transaction doubleSpendingTx = new Transaction(
                Collections.singletonList(input),
                Collections.singletonList(
                        new Output(
                                Simulation.randomHash(),
                                20L
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(PoolDoubleSpendingTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", doubleSpendingTx);
        facts.put("consensus", consensus);
        facts.put("transactionPool", transactionPool);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void PoolDoubleSpendingTxRuleSuccess() throws DatabaseException {
        Input input = new Input(
                Simulation.randomSignature(),
                Simulation.randomHash(),
                0
        );
        Transaction spendingTx = new Transaction(
                Collections.singletonList(Simulation.randomInput()),
                Collections.singletonList(
                        new Output(
                                Simulation.randomHash(),
                                20L
                        )
                )
        );

        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        transactionPool.addIndependentTransaction(spendingTx);

        Transaction doubleSpendingTx = new Transaction(
                Collections.singletonList(input),
                Collections.singletonList(
                        new Output(
                                Simulation.randomHash(),
                                20L
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(PoolDoubleSpendingTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", doubleSpendingTx);
        facts.put("consensus", consensus);
        facts.put("transactionPool", transactionPool);

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void SignatureTxRuleSuccess() throws DatabaseException {
        BigInteger privateKey = BigInteger.TEN;
        PublicKey publicKey = CURVE.getPublicKeyFromPrivateKey(privateKey);

        Hash coinbaseOutputAddress = publicKey.computeHash();

        Transaction coinbase = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(
                        new Output(
                                coinbaseOutputAddress,
                                20L
                        )
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

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(coinbase)
        );

        blockProcessor.processNewBlock(block);

        Transaction unsignedSpendingTx = new Transaction(
                Collections.singletonList(
                        new Input(
                                null,
                                coinbase.getHash(),
                                0
                        )
                ),
                Collections.emptyList()
        );
        Signature signature = signer.signMessage(unsignedSpendingTx.getSignableTransactionData(), privateKey);
        Transaction spendingTx = unsignedSpendingTx.getSignedTransaction(
                Collections.singletonMap(unsignedSpendingTx.getInputs().get(0), signature)
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(SignatureTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));
        facts.put("signer", signer);

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void SignatureTxRuleFailInvalid() throws DatabaseException {
        BigInteger privateKey = BigInteger.TEN;
        PublicKey publicKey = CURVE.getPublicKeyFromPrivateKey(privateKey);

        Hash coinbaseOutputAddress = publicKey.computeHash();

        Transaction coinbase = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(
                        new Output(
                                coinbaseOutputAddress,
                                20L
                        )
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

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(coinbase)
        );

        blockProcessor.processNewBlock(block);

        Transaction unsignedSpendingTx = new Transaction(
                Collections.singletonList(
                        new Input(
                                null,
                                coinbase.getHash(),
                                0
                        )
                ),
                Collections.emptyList()
        );
        Signature signature = signer.signMessage(unsignedSpendingTx.getSignableTransactionData(), BigInteger.ONE);
        Transaction spendingTx = unsignedSpendingTx.getSignedTransaction(
                Collections.singletonMap(unsignedSpendingTx.getInputs().get(0), signature)
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(SignatureTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));
        facts.put("signer", signer);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void SignatureTxRuleFailNull() throws DatabaseException {
        BigInteger privateKey = BigInteger.TEN;
        PublicKey publicKey = CURVE.getPublicKeyFromPrivateKey(privateKey);

        Hash coinbaseOutputAddress = publicKey.computeHash();

        Transaction coinbase = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(
                        new Output(
                                coinbaseOutputAddress,
                                20L
                        )
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

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(coinbase)
        );

        blockProcessor.processNewBlock(block);

        Transaction unsignedSpendingTx = new Transaction(
                Collections.singletonList(
                        new Input(
                                null,
                                coinbase.getHash(),
                                0
                        )
                ),
                Collections.emptyList()
        );
        Signature signature = null;
        Transaction spendingTx = unsignedSpendingTx.getSignedTransaction(
                Collections.singletonMap(unsignedSpendingTx.getInputs().get(0), signature)
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(SignatureTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));
        facts.put("signer", signer);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void SufficientInputTxRuleFailInsufficient() throws DatabaseException {
        Transaction coinbaseOne = new Transaction(
                Collections.emptyList(),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                20L
                        ),
                        new Output(
                                Simulation.randomHash(),
                                15L
                        )
                )
        );


        Transaction coinbaseTwo = new Transaction(
                Collections.emptyList(),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                25L
                        ),
                        new Output(
                                Simulation.randomHash(),
                                10L
                        )
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

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Arrays.asList(coinbaseOne, coinbaseTwo)
        );

        blockProcessor.processNewBlock(block);

        Transaction spendingTx = new Transaction(
                Arrays.asList(new Input(
                                Simulation.randomSignature(),
                                coinbaseOne.getHash(),
                                0
                        ),
                        new Input(
                                Simulation.randomSignature(),
                                coinbaseOne.getHash(),
                                1
                        ),
                        new Input(
                                Simulation.randomSignature(),
                                coinbaseTwo.getHash(),
                                1
                        )),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                40L
                        ),
                        new Output(
                                Simulation.randomHash(),
                                6L
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(SufficientInputTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void SufficientInputTxRuleFailExact() throws DatabaseException {
        Transaction coinbaseOne = new Transaction(
                Collections.emptyList(),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                20L
                        ),
                        new Output(
                                Simulation.randomHash(),
                                15L
                        )
                )
        );


        Transaction coinbaseTwo = new Transaction(
                Collections.emptyList(),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                25L
                        ),
                        new Output(
                                Simulation.randomHash(),
                                10L
                        )
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

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Arrays.asList(coinbaseOne, coinbaseTwo)
        );

        blockProcessor.processNewBlock(block);

        Transaction spendingTx = new Transaction(
                Arrays.asList(new Input(
                                Simulation.randomSignature(),
                                coinbaseOne.getHash(),
                                0
                        ),
                        new Input(
                                Simulation.randomSignature(),
                                coinbaseOne.getHash(),
                                1
                        ),
                        new Input(
                                Simulation.randomSignature(),
                                coinbaseTwo.getHash(),
                                1
                        )),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                40L
                        ),
                        new Output(
                                Simulation.randomHash(),
                                5L
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(SufficientInputTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void SufficientInputTxRuleSuccess() throws DatabaseException {
        Transaction coinbaseOne = new Transaction(
                Collections.emptyList(),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                20L
                        ),
                        new Output(
                                Simulation.randomHash(),
                                15L
                        )
                )
        );


        Transaction coinbaseTwo = new Transaction(
                Collections.emptyList(),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                25L
                        ),
                        new Output(
                                Simulation.randomHash(),
                                10L
                        )
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

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Arrays.asList(coinbaseOne, coinbaseTwo)
        );

        blockProcessor.processNewBlock(block);

        Transaction spendingTx = new Transaction(
                Arrays.asList(new Input(
                                Simulation.randomSignature(),
                                coinbaseOne.getHash(),
                                0
                        ),
                        new Input(
                                Simulation.randomSignature(),
                                coinbaseOne.getHash(),
                                1
                        ),
                        new Input(
                                Simulation.randomSignature(),
                                coinbaseTwo.getHash(),
                                1
                        )),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                40L
                        ),
                        new Output(
                                Simulation.randomHash(),
                                4L
                        )
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(SufficientInputTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidInputChainUTXOTxRuleFail() throws DatabaseException {
        Transaction coinbase = new Transaction(
                Collections.emptyList(),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                20L
                        ),
                        new Output(
                                Simulation.randomHash(),
                                15L
                        )
                )
        );

        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);

        Transaction spendingTx = new Transaction(
                Arrays.asList(new Input(
                        Simulation.randomSignature(),
                        coinbase.getHash(),
                        0
                )),
                Collections.singletonList(
                        Simulation.randomOutput()
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidInputUTXOTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", chainUtxoDatabase);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidInputChainUTXOTxRuleSuccess() throws DatabaseException {
        Transaction coinbase = new Transaction(
                Collections.emptyList(),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                20L
                        ),
                        new Output(
                                Simulation.randomHash(),
                                15L
                        )
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

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(coinbase)
        );

        blockProcessor.processNewBlock(block);

        Transaction spendingTx = new Transaction(
                Arrays.asList(new Input(
                        Simulation.randomSignature(),
                        coinbase.getHash(),
                        0
                )),
                Collections.singletonList(
                        Simulation.randomOutput()
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidInputUTXOTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));
        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidInputTxRuleSuccessChain() throws DatabaseException {
        Transaction coinbase = new Transaction(
                Collections.emptyList(),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                20L
                        ),
                        new Output(
                                Simulation.randomHash(),
                                15L
                        )
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

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(coinbase)
        );

        blockProcessor.processNewBlock(block);

        Transaction spendingTx = new Transaction(
                Arrays.asList(new Input(
                        Simulation.randomSignature(),
                        coinbase.getHash(),
                        0
                )),
                Collections.singletonList(
                        Simulation.randomOutput()
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidInputUTXOTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidInputTxRuleSuccessPool() throws DatabaseException {
        Transaction coinbase = new Transaction(
                Collections.emptyList(),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                20L
                        ),
                        new Output(
                                Simulation.randomHash(),
                                15L
                        )
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

        transactionProcessor.processNewTransaction(coinbase);

        Transaction spendingTx = new Transaction(
                Arrays.asList(new Input(
                        Simulation.randomSignature(),
                        coinbase.getHash(),
                        0
                )),
                Collections.singletonList(
                        Simulation.randomOutput()
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidInputUTXOTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void ValidInputTxRuleFail() throws DatabaseException {
        Transaction coinbase = new Transaction(
                Collections.emptyList(),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                20L
                        ),
                        new Output(
                                Simulation.randomHash(),
                                15L
                        )
                )
        );

        Transaction coinbaseTwo = new Transaction(
                Collections.emptyList(),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                20L
                        ),
                        new Output(
                                Simulation.randomHash(),
                                15L
                        )
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

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(coinbase)
        );

        blockProcessor.processNewBlock(block);

        transactionProcessor.processNewTransaction(coinbaseTwo);

        Transaction spendingTx = new Transaction(
                Arrays.asList(
                        new Input(
                                Simulation.randomSignature(),
                                coinbase.getHash(),
                                0
                        ),
                        new Input(
                                Simulation.randomSignature(),
                                Simulation.randomHash(),
                                0
                        )),
                Collections.singletonList(
                        Simulation.randomOutput()
                )
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidInputUTXOTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(poolUtxo, chainUtxoDatabase));

        
        assertFalse(ruleBook.run(facts).isPassed());
    }
}
