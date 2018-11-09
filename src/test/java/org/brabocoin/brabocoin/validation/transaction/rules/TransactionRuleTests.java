package org.brabocoin.brabocoin.validation.transaction.rules;

import com.deliveredtechnologies.rulebook.FactMap;
import com.deliveredtechnologies.rulebook.NameValueReferableMap;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.dal.*;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.processor.BlockProcessor;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.processor.UTXOProcessor;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.validation.BraboRuleBook;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionRuleTests {
    static BraboConfig defaultConfig = BraboConfigProvider.getConfig().bind("brabo", BraboConfig.class);
    Consensus consensus = new Consensus();

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
    }

    @Test
    void CoinbaseCreationTxRuleFail() {
        Transaction transaction = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(Simulation.randomOutput())
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(CoinbaseCreationTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
    }

    @Test
    void CoinbaseCreationTxRuleSuccess() {
        Transaction transaction = new Transaction(
                Collections.singletonList(Simulation.randomInput()),
                Collections.singletonList(Simulation.randomOutput())
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(CoinbaseCreationTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertTrue(ruleBook.passed());
    }

    @Test
    void CoinbaseMaturityTxRuleFail() throws DatabaseException {
        Output output = Simulation.randomOutput();
        Transaction coinbase = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(output)
        );

        Block block = new Block(
                consensus.getGenesisBlock().computeHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomByteString(),
                0,
                1,
                Collections.singletonList(coinbase)
        );

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        BlockValidator blockValidator = new BlockValidator();
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        TransactionProcessor transactionProcessor = new TransactionProcessor(new TransactionValidator(),
                transactionPool, chainUtxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        blockProcessor.processNewBlock(block);

        Transaction spendCoinbase = new Transaction(
                Collections.singletonList(new Input(Simulation.randomSignature(), coinbase.computeHash(), 0)),
                Collections.singletonList(Simulation.randomOutput())
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(CoinbaseMaturityTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("mainChain", blockchain.getMainChain());
        facts.setValue("chainUTXODatabase", chainUtxoDatabase);
        facts.setValue("transaction", spendCoinbase);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
    }

    @Test
    void CoinbaseMaturityTxRuleSuccess() throws DatabaseException {
        Output output = Simulation.randomOutput();
        Transaction coinbase = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(output)
        );

        Block block = new Block(
                consensus.getGenesisBlock().computeHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomByteString(),
                0,
                1,
                Collections.singletonList(coinbase)
        );
        List<Block> blockList = new ArrayList<Block>() {{
            add(block);
        }};
        blockList.addAll(Simulation.randomBlockChainGenerator(100, block.computeHash(), 2, 0, 5));

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        BlockValidator blockValidator = new BlockValidator();
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        TransactionProcessor transactionProcessor = new TransactionProcessor(new TransactionValidator(),
                transactionPool, chainUtxoDatabase, new UTXODatabase(new HashMapDB()));
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
                Collections.singletonList(new Input(Simulation.randomSignature(), coinbase.computeHash(), 0)),
                Collections.singletonList(Simulation.randomOutput())
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(CoinbaseMaturityTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("mainChain", blockchain.getMainChain());
        facts.setValue("chainUTXODatabase", chainUtxoDatabase);
        facts.setValue("transaction", spendCoinbase);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertTrue(ruleBook.passed());
    }

    @Test
    void DuplicateInputTxRuleFail() {
        Input input = Simulation.randomInput();
        Transaction transaction = new Transaction(
                Arrays.asList(input, input),
                Collections.singletonList(Simulation.randomOutput())
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(DuplicateInputTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
    }

    @Test
    void DuplicateInputTxRuleSuccess() {
        Input input = Simulation.randomInput();
        Transaction transaction = new Transaction(
                Simulation.repeatedBuilder(Simulation::randomInput, 100),
                Collections.singletonList(Simulation.randomOutput())
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(DuplicateInputTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertTrue(ruleBook.passed());
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

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(DuplicatePoolTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);
        facts.setValue("pool", transactionPool);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
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

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(DuplicatePoolTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);
        facts.setValue("pool", transactionPool);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
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

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(DuplicatePoolTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);
        facts.setValue("pool", transactionPool);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
    }

    @Test
    void DuplicatePoolTxRuleSuccess() {
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());

        Input input = Simulation.randomInput();
        Transaction transaction = new Transaction(
                Arrays.asList(input, input),
                Collections.singletonList(Simulation.randomOutput())
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(DuplicatePoolTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);
        facts.setValue("pool", transactionPool);

        ruleBook.run(facts);
        assertTrue(ruleBook.passed());
    }

    @Test
    void OutputCountTxRuleCoinbaseFail() {
        Transaction transaction = new Transaction(
                Collections.emptyList(),
                Collections.emptyList()
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(OutputCountTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
    }

    @Test
    void OutputCountTxRuleCoinbaseSuccess() {
        Transaction transaction = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(Simulation.randomOutput())
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(OutputCountTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertTrue(ruleBook.passed());
    }

    @Test
    void OutputCountTxRuleNormalFailInputs() {
        Transaction transaction = new Transaction(
                Collections.emptyList(),
                Arrays.asList(Simulation.randomOutput(), Simulation.randomOutput())
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(OutputCountTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
    }

    @Test
    void OutputCountTxRuleNormalFailOutputs() {
        Transaction transaction = new Transaction(
                Collections.singletonList(Simulation.randomInput()),
                Collections.emptyList()
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(OutputCountTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
    }

    @Test
    void OutputCountTxRuleNormalSuccess() {
        Transaction transaction = new Transaction(
                Collections.singletonList(Simulation.randomInput()),
                Arrays.asList(Simulation.randomOutput(), Simulation.randomOutput())
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(OutputCountTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertTrue(ruleBook.passed());
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
        BlockValidator blockValidator = new BlockValidator();
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        TransactionProcessor transactionProcessor = new TransactionProcessor(new TransactionValidator(),
                transactionPool, chainUtxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        Block block = new Block(
                consensus.getGenesisBlock().computeHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomByteString(),
                0,
                1,
                Collections.singletonList(negativeOutputTransaction)
        );

        blockProcessor.processNewBlock(block);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        Simulation.randomSignature(),
                        negativeOutputTransaction.computeHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput())
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(InputValueTxRange.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", spendingTransaction);
        facts.setValue("consensus", consensus);
        facts.setValue("transactionProcessor", transactionProcessor);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
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

        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        TransactionProcessor transactionProcessor = new TransactionProcessor(new TransactionValidator(),
                transactionPool, chainUtxoDatabase, new UTXODatabase(new HashMapDB()));

        transactionProcessor.processNewTransaction(negativeOutputTransaction);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        Simulation.randomSignature(),
                        negativeOutputTransaction.computeHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput())
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(InputValueTxRange.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", spendingTransaction);
        facts.setValue("consensus", consensus);
        facts.setValue("transactionProcessor", transactionProcessor);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
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
        BlockValidator blockValidator = new BlockValidator();
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        TransactionProcessor transactionProcessor = new TransactionProcessor(new TransactionValidator(),
                transactionPool, chainUtxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        Block block = new Block(
                consensus.getGenesisBlock().computeHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomByteString(),
                0,
                1,
                Collections.singletonList(negativeOutputTransaction)
        );

        blockProcessor.processNewBlock(block);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        Simulation.randomSignature(),
                        negativeOutputTransaction.computeHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput())
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(InputValueTxRange.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", spendingTransaction);
        facts.setValue("consensus", consensus);
        facts.setValue("transactionProcessor", transactionProcessor);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
    }

    @Test
    void InputValueTxRangeFailExceedRangeSum() throws DatabaseException {
        Transaction negativeOutputTransaction = new Transaction(
                Collections.emptyList(),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                consensus.getMaxTransactionRange() / 3L
                        ), new Output(
                                Simulation.randomHash(),
                                consensus.getMaxTransactionRange() / 3L
                        ), new Output(
                                Simulation.randomHash(),
                                consensus.getMaxTransactionRange() / 3L + 5
                        )
                )
        );

        BlockDatabase blockDatabase = new BlockDatabase(new HashMapDB(), defaultConfig);
        Blockchain blockchain = new Blockchain(blockDatabase, consensus);
        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUtxoDatabase);
        BlockValidator blockValidator = new BlockValidator();
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        TransactionProcessor transactionProcessor = new TransactionProcessor(new TransactionValidator(),
                transactionPool, chainUtxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        Block block = new Block(
                consensus.getGenesisBlock().computeHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomByteString(),
                0,
                1,
                Collections.singletonList(negativeOutputTransaction)
        );

        blockProcessor.processNewBlock(block);

        Transaction spendingTransaction = new Transaction(
                Arrays.asList(new Input(
                                Simulation.randomSignature(),
                                negativeOutputTransaction.computeHash(),
                                0
                        ),
                        new Input(
                                Simulation.randomSignature(),
                                negativeOutputTransaction.computeHash(),
                                1
                        ),
                        new Input(
                                Simulation.randomSignature(),
                                negativeOutputTransaction.computeHash(),
                                2
                        )),
                Collections.singletonList(Simulation.randomOutput())
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(InputValueTxRange.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", spendingTransaction);
        facts.setValue("consensus", consensus);
        facts.setValue("transactionProcessor", transactionProcessor);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
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

        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        TransactionProcessor transactionProcessor = new TransactionProcessor(new TransactionValidator(),
                transactionPool, chainUtxoDatabase, new UTXODatabase(new HashMapDB()));

        transactionProcessor.processNewTransaction(zeroOutputTransaction);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        Simulation.randomSignature(),
                        zeroOutputTransaction.computeHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput())
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(InputValueTxRange.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", spendingTransaction);
        facts.setValue("consensus", consensus);
        facts.setValue("transactionProcessor", transactionProcessor);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
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
        BlockValidator blockValidator = new BlockValidator();
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        TransactionProcessor transactionProcessor = new TransactionProcessor(new TransactionValidator(),
                transactionPool, chainUtxoDatabase, new UTXODatabase(new HashMapDB()));
        BlockProcessor blockProcessor = new BlockProcessor(
                blockchain,
                utxoProcessor,
                transactionProcessor,
                consensus,
                blockValidator);

        Block block = new Block(
                consensus.getGenesisBlock().computeHash(),
                Simulation.randomHash(),
                Simulation.randomHash(),
                Simulation.randomByteString(),
                0,
                1,
                Collections.singletonList(positiveOutputTransaction)
        );

        blockProcessor.processNewBlock(block);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        Simulation.randomSignature(),
                        positiveOutputTransaction.computeHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput())
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(InputValueTxRange.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", spendingTransaction);
        facts.setValue("consensus", consensus);
        facts.setValue("transactionProcessor", transactionProcessor);

        ruleBook.run(facts);
        assertTrue(ruleBook.passed());
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

        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);
        TransactionPool transactionPool = new TransactionPool(defaultConfig, new Random());
        TransactionProcessor transactionProcessor = new TransactionProcessor(new TransactionValidator(),
                transactionPool, chainUtxoDatabase, new UTXODatabase(new HashMapDB()));

        transactionProcessor.processNewTransaction(positiveOutputTransaction);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        Simulation.randomSignature(),
                        positiveOutputTransaction.computeHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput())
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(InputValueTxRange.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", spendingTransaction);
        facts.setValue("consensus", consensus);
        facts.setValue("transactionProcessor", transactionProcessor);

        ruleBook.run(facts);
        assertTrue(ruleBook.passed());
    }

    @Test
    void MaxSizeTxRuleFail() {
        Transaction transaction = new Transaction(
                Simulation.repeatedBuilder(Simulation::randomInput, 10000),
                Simulation.repeatedBuilder(Simulation::randomOutput, 10000)
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(MaxSizeTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
    }

    @Test
    void MaxSizeTxRuleSuccess() {
        Transaction transaction = new Transaction(
                Simulation.repeatedBuilder(Simulation::randomInput, 5),
                Simulation.repeatedBuilder(Simulation::randomOutput, 5)
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(MaxSizeTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertTrue(ruleBook.passed());
    }

    @Test
    void OutputValueTxRuleFailNegative() {
        Transaction transaction = new Transaction(
                Collections.singletonList(Simulation.randomInput()),
                Collections.singletonList(new Output(Simulation.randomHash(), -1))
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(OutputValueTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
    }

    @Test
    void OutputValueTxRuleFailZero() {
        Transaction transaction = new Transaction(
                Collections.singletonList(Simulation.randomInput()),
                Collections.singletonList(new Output(Simulation.randomHash(), 0))
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(OutputValueTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
    }

    @Test
    void OutputValueTxRuleFailExceedSingle() {
        Transaction transaction = new Transaction(
                Collections.singletonList(Simulation.randomInput()),
                Collections.singletonList(new Output(Simulation.randomHash(), consensus.getMaxTransactionRange() + 1))
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(OutputValueTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
    }

    @Test
    void OutputValueTxRuleFailExceedSum() {
        Transaction transaction = new Transaction(
                Collections.singletonList(Simulation.randomInput()),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                consensus.getMaxTransactionRange() / 3L
                        ), new Output(
                                Simulation.randomHash(),
                                consensus.getMaxTransactionRange() / 3L
                        ), new Output(
                                Simulation.randomHash(),
                                consensus.getMaxTransactionRange() / 3L + 5
                        )
                )
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(OutputValueTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertFalse(ruleBook.passed());
    }

    @Test
    void OutputValueTxRuleSuccess() {
        Transaction transaction = new Transaction(
                Collections.singletonList(Simulation.randomInput()),
                Arrays.asList(
                        new Output(
                                Simulation.randomHash(),
                                consensus.getMaxTransactionRange() / 3L
                        )
                )
        );

        BraboRuleBook ruleBook = new BraboRuleBook(
                Collections.singletonList(OutputValueTxRule.class)
        );

        NameValueReferableMap facts = new FactMap<>();
        facts.setValue("transaction", transaction);
        facts.setValue("consensus", consensus);

        ruleBook.run(facts);
        assertTrue(ruleBook.passed());
    }
}