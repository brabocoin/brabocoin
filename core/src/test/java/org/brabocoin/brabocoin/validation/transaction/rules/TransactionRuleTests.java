package org.brabocoin.brabocoin.validation.transaction.rules;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.dal.CompositeReadonlyUTXOSet;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Signature;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.UnsignedTransaction;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.testutil.TestState;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.block.BlockValidationResult;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.brabocoin.brabocoin.validation.fact.FactMap;
import org.brabocoin.brabocoin.validation.rule.RuleBook;
import org.brabocoin.brabocoin.validation.rule.RuleList;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidationResult;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionRuleTests {
    static BraboConfig defaultConfig = BraboConfigProvider.getConfig().bind("brabo", BraboConfig.class);
    Consensus consensus = new Consensus();

    private static final ByteString ZERO = ByteString.copyFrom(new byte[]{0});
    private static final EllipticCurve CURVE = EllipticCurve.secp256k1();

    private static Signer signer;

    private State state;


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

    @BeforeEach
    void setUpState() throws DatabaseException {
        state = new TestState(defaultConfig) {
            @Override
            protected BlockValidator createBlockValidator() {
                return new BlockValidator(
                        this
                ) {
                    @Override
                    public BlockValidationResult checkConnectBlockValid(@NotNull Block block) {
                        return BlockValidationResult.passed();
                    }

                    @Override
                    public BlockValidationResult checkIncomingBlockValid(@NotNull Block block) {
                        return BlockValidationResult.passed();
                    }

                    @Override
                    public BlockValidationResult checkPostOrphanBlockValid(@NotNull Block block) {
                        return BlockValidationResult.passed();
                    }
                };
            }

            @Override
            protected TransactionValidator createTransactionValidator() {
                return new TransactionValidator(
                        this
                ) {
                    @Override
                    public TransactionValidationResult checkTransactionBlockContextual(@NotNull Transaction transaction) {
                        return TransactionValidationResult.passed();
                    }

                    @Override
                    public TransactionValidationResult checkTransactionBlockNonContextual(@NotNull Transaction transaction) {
                        return TransactionValidationResult.passed();
                    }

                    @Override
                    public TransactionValidationResult checkTransactionPostOrphan(@NotNull Transaction transaction) {
                        return TransactionValidationResult.passed();
                    }

                    @Override
                    public TransactionValidationResult checkTransactionValid(@NotNull Transaction transaction) {
                        return TransactionValidationResult.passed();
                    }
                };
            }
        };
    }

    @Test
    void CoinbaseCreationTxRuleFail() {
        Transaction transaction = Transaction.coinbase(Simulation.randomOutput(),0);
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
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
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
        Transaction coinbase = Transaction.coinbase(output,0);

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(coinbase),
                0);

        state.getBlockProcessor().processNewBlock(block);

        Transaction spendCoinbase = new Transaction(
                Collections.singletonList(new Input(coinbase.getHash(), 0)),
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(CoinbaseMaturityTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("mainChain", state.getBlockchain().getMainChain());
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));
        facts.put("transaction", spendCoinbase);
        facts.put("consensus", consensus);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void CoinbaseMaturityTxRuleSuccess() throws DatabaseException {
        Output output = Simulation.randomOutput();
        Transaction coinbase = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(output), Collections.emptyList()
        );

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(coinbase),
                0);
        List<Block> blockList = new ArrayList<Block>() {{
            add(block);
        }};
        blockList.addAll(Simulation.randomBlockChainGenerator(100, block.getHash(), 2, 0, 5));

        for (Block b : blockList) {
            state.getBlockProcessor().processNewBlock(b);
        }

        Transaction spendCoinbase = new Transaction(
                Collections.singletonList(new Input(coinbase.getHash(), 0)),
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(CoinbaseMaturityTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("mainChain", state.getBlockchain().getMainChain());
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));
        facts.put("transaction", spendCoinbase);
        facts.put("consensus", consensus);

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void DuplicateInputTxRuleFail() {
        Input input = Simulation.randomInput();
        Transaction transaction = new Transaction(
                Arrays.asList(input, input),
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
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
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
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
        TransactionPool transactionPool = new TransactionPool(defaultConfig.maxTransactionPoolSize(), defaultConfig.maxOrphanTransactions(), new Random());


        Input input = Simulation.randomInput();
        Transaction transaction = new Transaction(
                Arrays.asList(input, input),
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
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
        TransactionPool transactionPool = new TransactionPool(defaultConfig.maxTransactionPoolSize(), defaultConfig.maxOrphanTransactions(), new Random());


        Input input = Simulation.randomInput();
        Transaction transaction = new Transaction(
                Arrays.asList(input, input),
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
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
        TransactionPool transactionPool = new TransactionPool(defaultConfig.maxTransactionPoolSize(), defaultConfig.maxOrphanTransactions(), new Random());


        Input input = Simulation.randomInput();
        Transaction transaction = new Transaction(
                Arrays.asList(input, input),
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
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
        TransactionPool transactionPool = new TransactionPool(defaultConfig.maxTransactionPoolSize(), defaultConfig.maxOrphanTransactions(), new Random());

        Input input = Simulation.randomInput();
        Transaction transaction = new Transaction(
                Arrays.asList(input, input),
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
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
                Collections.emptyList(), Collections.emptyList()
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
        Transaction transaction = Transaction.coinbase(Simulation.randomOutput(), 1);

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
                Arrays.asList(Simulation.randomOutput(), Simulation.randomOutput()), Collections.emptyList()
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
                Collections.emptyList(), Collections.emptyList()
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
                Arrays.asList(Simulation.randomOutput(), Simulation.randomOutput()), Collections.emptyList()
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
                )), Collections.emptyList()
        );

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(negativeOutputTransaction),
                0);

        state.getBlockProcessor().processNewBlock(block);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        
                        negativeOutputTransaction.getHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputValueTxRange.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTransaction);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void InputValueTxRangeFailNegativeTransactionProcessor() throws DatabaseException {
        Transaction negativeOutputTransaction = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(new Output(
                        Simulation.randomHash(),
                        -1
                )), Collections.emptyList()
        );


        state.getTransactionProcessor().processNewTransaction(negativeOutputTransaction);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        
                        negativeOutputTransaction.getHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputValueTxRange.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTransaction);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void InputValueTxRangeFailExceedRangeSingleOutput() throws DatabaseException {
        Transaction negativeOutputTransaction = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(new Output(
                        Simulation.randomHash(),
                        Long.MAX_VALUE - 1
                )), Collections.emptyList()
        );


        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(negativeOutputTransaction),
                0);

        state.getBlockProcessor().processNewBlock(block);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        
                        negativeOutputTransaction.getHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputValueTxRange.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTransaction);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));

        
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
                ), Collections.emptyList()
        );


        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(negativeOutputTransaction),
                0);

        state.getBlockProcessor().processNewBlock(block);

        Transaction spendingTransaction = new Transaction(
                Arrays.asList(new Input(
                                
                                negativeOutputTransaction.getHash(),
                                0
                        ),
                        new Input(
                                
                                negativeOutputTransaction.getHash(),
                                1
                        ),
                        new Input(
                                
                                negativeOutputTransaction.getHash(),
                                2
                        )),
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputValueTxRange.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTransaction);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void InputValueTxRangeFailZeroOutput() throws DatabaseException {
        Transaction zeroOutputTransaction = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(new Output(
                        Simulation.randomHash(),
                        0
                )), Collections.emptyList()
        );

        state.getTransactionProcessor().processNewTransaction(zeroOutputTransaction);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        
                        zeroOutputTransaction.getHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputValueTxRange.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTransaction);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void InputValueTxRangeSuccessBlockProcessor() throws DatabaseException {
        Transaction positiveOutputTransaction = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(new Output(
                        Simulation.randomHash(),
                        1
                )), Collections.emptyList()
        );

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(positiveOutputTransaction),
                0);

        state.getBlockProcessor().processNewBlock(block);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        
                        positiveOutputTransaction.getHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputValueTxRange.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTransaction);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void InputValueTxRangeSuccessTransactionProcessor() throws DatabaseException {
        Transaction positiveOutputTransaction = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(new Output(
                        Simulation.randomHash(),
                        1
                )), Collections.emptyList()
        );
        state.getTransactionProcessor().processNewTransaction(positiveOutputTransaction);

        Transaction spendingTransaction = new Transaction(
                Collections.singletonList(new Input(
                        
                        positiveOutputTransaction.getHash(),
                        0
                )),
                Collections.singletonList(Simulation.randomOutput()), Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(InputValueTxRange.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTransaction);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void MaxSizeTxRuleFail() {
        Transaction transaction = new Transaction(
                Simulation.repeatedBuilder(Simulation::randomInput, 10000),
                Simulation.repeatedBuilder(Simulation::randomOutput, 10000), Collections.emptyList()
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
                Simulation.repeatedBuilder(Simulation::randomOutput, 5), Collections.emptyList()
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
                Collections.singletonList(new Output(Simulation.randomHash(), -1)), Collections.emptyList()
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
                Collections.singletonList(new Output(Simulation.randomHash(), 0)), Collections.emptyList()
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
                Collections.singletonList(new Output(Simulation.randomHash(), consensus.getMaxMoneyValue() + 1)),
                Collections.emptyList()
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
                ), Collections.emptyList()
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
                ), Collections.emptyList()
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
                ), Collections.emptyList()
        );

        TransactionPool transactionPool = new TransactionPool(defaultConfig.maxTransactionPoolSize(), defaultConfig.maxOrphanTransactions(), new Random());;
        transactionPool.addIndependentTransaction(spendingTx);

        Transaction doubleSpendingTx = new Transaction(
                Collections.singletonList(input),
                Collections.singletonList(
                        new Output(
                                Simulation.randomHash(),
                                20L
                        )
                ), Collections.emptyList()
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
                ), Collections.emptyList()
        );

        TransactionPool transactionPool = new TransactionPool(defaultConfig.maxTransactionPoolSize(), defaultConfig.maxOrphanTransactions(), new Random());
        transactionPool.addIndependentTransaction(spendingTx);

        Transaction doubleSpendingTx = new Transaction(
                Collections.singletonList(input),
                Collections.singletonList(
                        new Output(
                                Simulation.randomHash(),
                                20L
                        )
                ), Collections.emptyList()
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

        Hash coinbaseOutputAddress = publicKey.getHash();

        Transaction coinbase = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(
                        new Output(
                                coinbaseOutputAddress,
                                20L
                        )
                ), Collections.emptyList()
        );

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(coinbase),
                0);

        state.getBlockProcessor().processNewBlock(block);

        UnsignedTransaction unsignedSpendingTx = new UnsignedTransaction(
                Collections.singletonList(
                        new Input(
                                coinbase.getHash(),
                                0
                        )
                ),
                Collections.emptyList()
        );
        Signature signature = signer.signMessage(unsignedSpendingTx.getSignableTransactionData(), privateKey);
        Transaction spendingTx = unsignedSpendingTx.sign(
                Collections.singletonList(signature)
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(SignatureTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));
        facts.put("signer", signer);

        
        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void SignatureTxRuleFailInvalid() throws DatabaseException {
        BigInteger privateKey = BigInteger.TEN;
        PublicKey publicKey = CURVE.getPublicKeyFromPrivateKey(privateKey);

        Hash coinbaseOutputAddress = publicKey.getHash();

        Transaction coinbase = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(
                        new Output(
                                coinbaseOutputAddress,
                                20L
                        )
                ), Collections.emptyList()
        );

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(coinbase),
                0);

        state.getBlockProcessor().processNewBlock(block);

        UnsignedTransaction unsignedSpendingTx = new UnsignedTransaction(
                Collections.singletonList(
                        new Input(
                                coinbase.getHash(),
                                0
                        )
                ),
                Collections.emptyList()
        );
        Signature signature = signer.signMessage(unsignedSpendingTx.getSignableTransactionData(), BigInteger.ONE);
        Transaction spendingTx = unsignedSpendingTx.sign(
                Collections.singletonList(signature)
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(SignatureTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));
        facts.put("signer", signer);

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void SignatureTxRuleFailNull() throws DatabaseException {
        BigInteger privateKey = BigInteger.TEN;
        PublicKey publicKey = CURVE.getPublicKeyFromPrivateKey(privateKey);

        Hash coinbaseOutputAddress = publicKey.getHash();

        Transaction coinbase = new Transaction(
                Collections.emptyList(),
                Collections.singletonList(
                        new Output(
                                coinbaseOutputAddress,
                                20L
                        )
                ), Collections.emptyList()
        );

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(coinbase),
                0);

        state.getBlockProcessor().processNewBlock(block);

        UnsignedTransaction unsignedSpendingTx = new UnsignedTransaction(
                Collections.singletonList(
                        new Input(
                                coinbase.getHash(),
                                0
                        )
                ),
                Collections.emptyList()
        );
        Signature signature = null;
        Transaction spendingTx = unsignedSpendingTx.sign(
                Collections.singletonList(signature)
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(SignatureTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));
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
                ), Collections.emptyList()
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
                ), Collections.emptyList()
        );

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Arrays.asList(coinbaseOne, coinbaseTwo),
                0);

        state.getBlockProcessor().processNewBlock(block);

        Transaction spendingTx = new Transaction(
                Arrays.asList(new Input(
                                
                                coinbaseOne.getHash(),
                                0
                        ),
                        new Input(
                                
                                coinbaseOne.getHash(),
                                1
                        ),
                        new Input(
                                
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
                ), Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(SufficientInputTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));

        
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
                ), Collections.emptyList()
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
                ), Collections.emptyList()
        );

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Arrays.asList(coinbaseOne, coinbaseTwo),
                0);

        state.getBlockProcessor().processNewBlock(block);

        Transaction spendingTx = new Transaction(
                Arrays.asList(new Input(
                                
                                coinbaseOne.getHash(),
                                0
                        ),
                        new Input(
                                
                                coinbaseOne.getHash(),
                                1
                        ),
                        new Input(
                                
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
                ), Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(SufficientInputTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));

        
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
                ), Collections.emptyList()
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
                ), Collections.emptyList()
        );

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Arrays.asList(coinbaseOne, coinbaseTwo),
                0);

        state.getBlockProcessor().processNewBlock(block);

        Transaction spendingTx = new Transaction(
                Arrays.asList(new Input(
                                
                                coinbaseOne.getHash(),
                                0
                        ),
                        new Input(
                                
                                coinbaseOne.getHash(),
                                1
                        ),
                        new Input(
                                
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
                ), Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(SufficientInputTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));

        
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
                ), Collections.emptyList()
        );

        ChainUTXODatabase chainUtxoDatabase = new ChainUTXODatabase(new HashMapDB(), consensus);

        Transaction spendingTx = new Transaction(
                Arrays.asList(new Input(
                        
                        coinbase.getHash(),
                        0
                )),
                Collections.singletonList(
                        Simulation.randomOutput()
                ), Collections.emptyList()
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
                ), Collections.emptyList()
        );

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(coinbase),
                0);

        state.getBlockProcessor().processNewBlock(block);

        Transaction spendingTx = new Transaction(
                Arrays.asList(new Input(
                        
                        coinbase.getHash(),
                        0
                )),
                Collections.singletonList(
                        Simulation.randomOutput()
                ), Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidInputUTXOTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));
        
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
                ), Collections.emptyList()
        );

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(coinbase),
                0);

        state.getBlockProcessor().processNewBlock(block);

        Transaction spendingTx = new Transaction(
                Arrays.asList(new Input(
                        
                        coinbase.getHash(),
                        0
                )),
                Collections.singletonList(
                        Simulation.randomOutput()
                ), Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidInputUTXOTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));

        
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
                ), Collections.emptyList()
        );

        state.getTransactionProcessor().processNewTransaction(coinbase);

        Transaction spendingTx = new Transaction(
                Arrays.asList(new Input(
                        
                        coinbase.getHash(),
                        0
                )),
                Collections.singletonList(
                        Simulation.randomOutput()
                ), Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidInputUTXOTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));

        
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
                ), Collections.emptyList()
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
                ), Collections.emptyList()
        );

        Block block = new Block(
                consensus.getGenesisBlock().getHash(),
                Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
                1,
                Collections.singletonList(coinbase),
                0);

        state.getBlockProcessor().processNewBlock(block);

        state.getTransactionProcessor().processNewTransaction(coinbaseTwo);

        Transaction spendingTx = new Transaction(
                Arrays.asList(
                        new Input(
                                coinbase.getHash(),
                                0
                        ),
                        new Input(
                                
                                Simulation.randomHash(),
                                0
                        )),
                Collections.singletonList(
                        Simulation.randomOutput()
                ), Collections.emptyList()
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
                Collections.singletonList(ValidInputUTXOTxRule.class)
        ));

        FactMap facts = new FactMap();
        facts.put("transaction", spendingTx);
        facts.put("consensus", consensus);
        facts.put("utxoSet", new CompositeReadonlyUTXOSet(state.getPoolUTXODatabase(), state.getChainUTXODatabase()));

        
        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void SignatureCountTxRuleSuccess() {
        Transaction tx = new Transaction(
            Simulation.repeatedBuilder(Simulation::randomInput, 55),
            Collections.singletonList(
                Simulation.randomOutput()
            ),
            Simulation.repeatedBuilder(Simulation::randomSignature, 55)
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(SignatureCountTxRule.class)
        ));


        FactMap facts = new FactMap();
        facts.put("transaction", tx);
        facts.put("consensus", consensus);

        assertTrue(ruleBook.run(facts).isPassed());
    }

    @Test
    void SignatureCountTxRuleFailLess() {
        Transaction tx = new Transaction(
            Simulation.repeatedBuilder(Simulation::randomInput, 55),
            Collections.singletonList(
                Simulation.randomOutput()
            ),
            Simulation.repeatedBuilder(Simulation::randomSignature, 44)
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(SignatureCountTxRule.class)
        ));


        FactMap facts = new FactMap();
        facts.put("transaction", tx);
        facts.put("consensus", consensus);

        assertFalse(ruleBook.run(facts).isPassed());
    }

    @Test
    void SignatureCountTxRuleFailMore() {
        Transaction tx = new Transaction(
            Simulation.repeatedBuilder(Simulation::randomInput, 55),
            Collections.singletonList(
                Simulation.randomOutput()
            ),
            Simulation.repeatedBuilder(Simulation::randomSignature, 66)
        );

        RuleBook ruleBook = new RuleBook(new RuleList(
            Collections.singletonList(SignatureCountTxRule.class)
        ));


        FactMap facts = new FactMap();
        facts.put("transaction", tx);
        facts.put("consensus", consensus);

        assertFalse(ruleBook.run(facts).isPassed());
    }
}
