package org.brabocoin.brabocoin.processor;

import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.dal.UTXODatabase;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.*;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.ValidationStatus;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidationResult;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test {@link TransactionProcessor}.
 */
class TransactionProcessorTest {

    private static BraboConfig config;

    private TransactionValidator validator;
    private TransactionPool pool;
    private Consensus consensus;
    private ChainUTXODatabase utxoFromChain;
    private UTXODatabase utxoFromPool;
    private TransactionProcessor processor;

    @BeforeAll
    static void setUpConfig() {
        config = BraboConfigProvider.getConfig().bind("brabo", BraboConfig.class);
    }

    @BeforeEach
    void setUp() throws DatabaseException {
        pool = new TransactionPool(config, new Random());
        consensus = new Consensus();
        utxoFromChain = new ChainUTXODatabase(new HashMapDB(), consensus);
        utxoFromPool = new UTXODatabase(new HashMapDB());
        validator = new TransactionValidator(
                null,
                null,
                null,
                utxoFromChain,
                utxoFromPool,
                null
        );
        processor = new TransactionProcessor(validator, pool, utxoFromChain, utxoFromPool);
    }

    @Test
    @Disabled
    void processNewTransactionInvalid() throws DatabaseException {
        validator = new TransactionValidator(
                consensus,
                null,
                null,
                utxoFromChain,
                utxoFromPool,
                null
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


        processor = new TransactionProcessor(validator, pool, utxoFromChain, utxoFromPool);
        Transaction transaction = Simulation.randomTransaction(5, 5);

        ProcessedTransactionResult result = processor.processNewTransaction(transaction);

        assertEquals(ValidationStatus.INVALID, result.getStatus());
        assertTrue(result.getValidatedOrphans().isEmpty());
    }

    @Test
    void processNewTransactionAlreadyStoredIndependent() throws DatabaseException {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        pool.addIndependentTransaction(transaction);

        ProcessedTransactionResult result = processor.processNewTransaction(transaction);
        assertEquals(ValidationStatus.INVALID, result.getStatus());
        assertTrue(result.getValidatedOrphans().isEmpty());
    }

    @Test
    void processNewTransactionAlreadyStoredDependent() throws DatabaseException {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        pool.addDependentTransaction(transaction);

        ProcessedTransactionResult result = processor.processNewTransaction(transaction);
        assertEquals(ValidationStatus.INVALID, result.getStatus());
        assertTrue(result.getValidatedOrphans().isEmpty());
    }

    @Test
    void processNewTransactionAlreadyStoredOrphan() throws DatabaseException {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        pool.addOrphanTransaction(transaction);

        ProcessedTransactionResult result = processor.processNewTransaction(transaction);
        assertEquals(ValidationStatus.INVALID, result.getStatus());
        assertTrue(result.getValidatedOrphans().isEmpty());
    }

    @Test
    void processNewTransactionOrphan() throws DatabaseException {
        Transaction orphan = Simulation.randomTransaction(5, 5);
        Hash hash = orphan.getHash();

        ProcessedTransactionResult result = processor.processNewTransaction(orphan);
        assertEquals(ValidationStatus.ORPHAN, result.getStatus());
        assertTrue(result.getValidatedOrphans().isEmpty());

        for (int i = 0; i < orphan.getOutputs().size(); i++) {
            assertFalse(utxoFromPool.isUnspent(hash, i));
        }
    }

    @Test
    void processNewTransactionIndependent() throws DatabaseException {
        // Construct the chain UTXO such that the transaction becomes independent
        Transaction transaction = createIndependentTransaction();
        Hash hash = transaction.getHash();

        ProcessedTransactionResult result = processor.processNewTransaction(transaction);
        assertEquals(ValidationStatus.VALID, result.getStatus());
        assertTrue(result.getValidatedOrphans().isEmpty());

        for (int i = 0; i < transaction.getOutputs().size(); i++) {
            assertTrue(utxoFromPool.isUnspent(hash, i));
        }
    }

    private Transaction createIndependentTransaction() throws DatabaseException {
        Transaction transaction = Simulation.randomTransaction(5, 5);

        for (Input input : transaction.getInputs()) {
            utxoFromChain.addUnspentOutputInfo(
                input.getReferencedTransaction(),
                input.getReferencedOutputIndex(),
                new UnspentOutputInfo(false, 1, 1, Simulation.randomHash())
            );
        }

        return transaction;
    }

    @Test
    void processNewTransactionDependent() throws DatabaseException {
        Transaction transaction = createDependentTransaction();
        Hash hash = transaction.getHash();

        ProcessedTransactionResult result = processor.processNewTransaction(transaction);
        assertEquals(ValidationStatus.VALID, result.getStatus());
        assertTrue(result.getValidatedOrphans().isEmpty());

        for (int i = 0; i < transaction.getOutputs().size(); i++) {
            assertTrue(utxoFromPool.isUnspent(hash, i));
        }
    }

    @NotNull
    private Transaction createDependentTransaction() throws DatabaseException {
        Transaction transaction = new Transaction(
            IntStream.range(0, 2)
                .mapToObj(i -> Simulation.randomInput())
                .collect(Collectors.toList()),
            IntStream.range(0, 2)
                .mapToObj(i -> Simulation.randomOutput())
                .collect(Collectors.toList())
        );

        // Construct the chain UTXO such that the transaction becomes independent
        // Mark input as index 2 as dependent
        List<Input> inputs = transaction.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            Input input = inputs.get(i);

            if (i == 1) {
                utxoFromPool.addUnspentOutputInfo(
                    input.getReferencedTransaction(),
                    input.getReferencedOutputIndex(),
                    new UnspentOutputInfo(false, 1, 1, Simulation.randomHash())
                );
            } else {
                utxoFromChain.addUnspentOutputInfo(
                    input.getReferencedTransaction(),
                    input.getReferencedOutputIndex(),
                    new UnspentOutputInfo(false, 1, 1, Simulation.randomHash())
                );
            }
        }
        return transaction;
    }

    @Test
    void processNewTransactionIndependentAddOrphan() throws DatabaseException {
        // Construct the chain UTXO such that the transaction becomes independent
        Transaction transactionA = createIndependentTransaction();
        Hash hash = transactionA.getHash();

        // Add orphan such that B -> A
        Transaction transactionB = new Transaction(
            Collections.singletonList(new Input(Simulation.randomSignature(), hash, 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        processor.processNewTransaction(transactionB);

        ProcessedTransactionResult result = processor.processNewTransaction(transactionA);
        assertEquals(1, result.getValidatedOrphans().size());

        Hash hashB = transactionB.getHash();
        for (int i = 0; i < transactionB.getOutputs().size(); i++) {
            assertTrue(utxoFromPool.isUnspent(hashB, i));
        }
    }

    @Test
    void processNewTransactionDependentAddOrphan() throws DatabaseException {
        Transaction transactionA = createDependentTransaction();
        Hash hash = transactionA.getHash();

        // Add orphan such that B -> A
        Transaction transactionB = new Transaction(
            Collections.singletonList(new Input(Simulation.randomSignature(), hash, 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        processor.processNewTransaction(transactionB);

        ProcessedTransactionResult result = processor.processNewTransaction(transactionA);
        assertEquals(1, result.getValidatedOrphans().size());

        Hash hashB = transactionB.getHash();
        for (int i = 0; i < transactionB.getOutputs().size(); i++) {
            assertTrue(utxoFromPool.isUnspent(hashB, i));
        }
    }

    @Test
    void processNewTransactionAddChainedOrphans() throws DatabaseException {
        // Construct the chain UTXO such that the transaction becomes independent
        Transaction transactionA = createIndependentTransaction();
        Hash hash = transactionA.getHash();

        // Add orphans such that C -> B -> A
        Transaction transactionB = new Transaction(
            Collections.singletonList(new Input(Simulation.randomSignature(), hash, 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        Hash hashB = transactionB.getHash();
        processor.processNewTransaction(transactionB);

        Transaction transactionC = new Transaction(
            Collections.singletonList(new Input(Simulation.randomSignature(), hashB, 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        Hash hashC = transactionC.getHash();
        ProcessedTransactionResult resultC = processor.processNewTransaction(transactionC);

        assertEquals(ValidationStatus.ORPHAN, resultC.getStatus());

        ProcessedTransactionResult result = processor.processNewTransaction(transactionA);
        assertEquals(2, result.getValidatedOrphans().size());

        for (int i = 0; i < transactionC.getOutputs().size(); i++) {
            assertTrue(utxoFromPool.isUnspent(hashC, i));
        }
    }

    @Test
    void processNewTransactionIndependentAddOrphanInvalid() throws DatabaseException {
        // Construct the chain UTXO such that the transaction becomes independent
        Transaction transactionA = createIndependentTransaction();
        Hash hash = transactionA.getHash();

        // Add orphan such that B -> A, but with other dependencies such that the orphan is still invalid.
        Transaction transactionB = new Transaction(
            Arrays.asList(
                new Input(Simulation.randomSignature(), hash, 0),
                Simulation.randomInput()
            ),
            Collections.singletonList(Simulation.randomOutput())
        );
        processor.processNewTransaction(transactionB);

        ProcessedTransactionResult result = processor.processNewTransaction(transactionA);
        assertTrue(result.getValidatedOrphans().isEmpty());

        Hash hashB = transactionB.getHash();
        for (int i = 0; i < transactionB.getOutputs().size(); i++) {
            assertFalse(utxoFromPool.isUnspent(hashB, i));
        }
    }

    @Test
    void processNewTransactionDependentAddOrphanInvalid() throws DatabaseException {
        Transaction transactionA = createDependentTransaction();
        Hash hash = transactionA.getHash();

        // Add orphan such that B -> A, but with other dependencies such that the orphan is still invalid.
        Transaction transactionB = new Transaction(
            Arrays.asList(
                new Input(Simulation.randomSignature(), hash, 0),
                Simulation.randomInput()
            ),
            Collections.singletonList(Simulation.randomOutput())
        );
        processor.processNewTransaction(transactionB);

        ProcessedTransactionResult result = processor.processNewTransaction(transactionA);
        assertTrue(result.getValidatedOrphans().isEmpty());

        Hash hashB = transactionB.getHash();
        for (int i = 0; i < transactionB.getOutputs().size(); i++) {
            assertFalse(utxoFromPool.isUnspent(hashB, i));
        }
    }

    @Test
    void processTopBlockConnected() throws DatabaseException {
        Block block = Simulation.randomBlock(Simulation.randomHash(), 1, 5, 5, 5);

        // Add transactions to pool as independent forcefully
        for (Transaction transaction : block.getTransactions()) {
            pool.addIndependentTransaction(transaction);
            utxoFromPool.setOutputsUnspent(transaction, Constants.TRANSACTION_POOL_HEIGHT);

            // These transactions also exist in the blockchain now
            utxoFromChain.setOutputsUnspent(transaction, block.getBlockHeight());
        }

        Transaction tFromBlock = block.getTransactions().get(0);

        // Add a dependent transaction that will be promoted
        Transaction transactionA = new Transaction(
            Collections.singletonList(new Input(Simulation.randomSignature(), tFromBlock.getHash(), 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        Hash hashA = transactionA.getHash();
        pool.addDependentTransaction(transactionA);
        utxoFromPool.setOutputsUnspent(transactionA, Constants.TRANSACTION_POOL_HEIGHT);

        processor.processTopBlockConnected(block);

        for (Transaction transaction : block.getTransactions()) {
            Hash hash = transaction.getHash();
            assertFalse(pool.hasValidTransaction(hash));

            List<Output> outputs = transaction.getOutputs();
            for (int i = 0; i < outputs.size(); i++) {
                assertFalse(utxoFromPool.isUnspent(hash, i));
            }
        }

        // Assert promoted dependent transaction
        assertTrue(pool.isIndependent(hashA));
    }

    @Test
    void processTopBlockDisconnected() throws DatabaseException {
        Block block = Simulation.randomBlock(Simulation.randomHash(), 2, 5, 5, 5);

        // Add chain UTXO for the referenced transaction inputs
        for (Transaction transaction : block.getTransactions()) {
            for (Input input : transaction.getInputs()) {
                utxoFromChain.addUnspentOutputInfo(
                    input.getReferencedTransaction(),
                    input.getReferencedOutputIndex(),
                    new UnspentOutputInfo(false, 1, 10, Simulation.randomHash())
                );
            }
        }

        Transaction tFromBlock = block.getTransactions().get(0);
        Input iFromBlock = tFromBlock.getInputs().get(0);

        // Add independent transaction that will now become dependent
        Transaction independent = new Transaction(
            Collections.singletonList(new Input(Simulation.randomSignature(), tFromBlock.getHash(), 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        Hash independentHash = independent.getHash();
        pool.addIndependentTransaction(independent);
        utxoFromPool.setOutputsUnspent(independent, Constants.TRANSACTION_POOL_HEIGHT);

        // Add orphan transaction that will now become valid
        Transaction orphan = new Transaction(
            Collections.singletonList(new Input(Simulation.randomSignature(), iFromBlock.getReferencedTransaction(), iFromBlock.getReferencedOutputIndex())),
            Collections.singletonList(Simulation.randomOutput())
        );
        Hash orphanHash = orphan.getHash();
        pool.addOrphanTransaction(orphan);

        processor.processTopBlockDisconnected(block);

        for (Transaction transaction : block.getTransactions()) {
            Hash hash = transaction.getHash();
            assertTrue(pool.hasValidTransaction(hash));
        }

        // Check independent transaction is demoted
        assertTrue(pool.isDependent(independentHash));
        assertTrue(pool.isIndependent(orphanHash));
    }
}
