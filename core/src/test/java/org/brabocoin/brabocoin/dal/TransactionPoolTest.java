package org.brabocoin.brabocoin.dal;

import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test {@link TransactionPool}.
 */
class TransactionPoolTest {

    private static BraboConfig config;

    private TransactionPool pool;

    @BeforeAll
    static void setUpConfig() {
        config = BraboConfigProvider.getConfig().bind("brabo", BraboConfig.class);
    }

    @BeforeEach
    void setUp() {
        pool = new TransactionPool(config.maxTransactionPoolSize(), config.maxOrphanTransactions(),new Random());
    }

    @Test
    void addFindIndependentTransaction() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        pool.addIndependentTransaction(transaction);

        Transaction fromPool = pool.findValidatedTransaction(hash);
        assertNotNull(fromPool);
        assertEquals(hash, fromPool.getHash());
    }

    @Test
    void addFindDependentTransaction() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        pool.addDependentTransaction(transaction);

        Transaction fromPool = pool.findValidatedTransaction(hash);
        assertNotNull(fromPool);
        assertEquals(hash, fromPool.getHash());
    }

    @Test
    void addFindOrphan() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        pool.addOrphanTransaction(transaction);

        Transaction fromPool = pool.findOrphan(hash);
        assertNotNull(fromPool);
        assertEquals(hash, fromPool.getHash());
    }

    @Test
    void orphanIsNotValidated() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        pool.addOrphanTransaction(transaction);

        assertNull(pool.findValidatedTransaction(hash));
    }

    @Test
    void independentIsNotOrphan() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        pool.addIndependentTransaction(transaction);

        assertNull(pool.findOrphan(hash));
    }

    @Test
    void dependentIsNotOrphan() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        pool.addDependentTransaction(transaction);

        assertNull(pool.findOrphan(hash));
    }

    @Test
    void removeValidatedTransactionIndependent() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        pool.addIndependentTransaction(transaction);

        pool.removeValidatedTransaction(hash);

        assertFalse(pool.hasValidTransaction(hash));
    }

    @Test
    void removeValidatedTransactionDependent() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        pool.addDependentTransaction(transaction);

        pool.removeValidatedTransaction(hash);

        assertFalse(pool.hasValidTransaction(hash));
    }

    @Test
    void isDependent() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        pool.addDependentTransaction(transaction);

        assertTrue(pool.isDependent(hash));
    }

    @Test
    void isIndependent() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        pool.addIndependentTransaction(transaction);

        assertTrue(pool.isIndependent(hash));
    }

    @Test
    void removeValidOrphansFromParent() {
        Transaction transactionA = Simulation.randomTransaction(5, 5);
        Hash hashA = transactionA.getHash();
        pool.addOrphanTransaction(transactionA);

        Transaction transactionB = new Transaction(
            Collections.singletonList(new Input(Simulation.randomSignature(), hashA, 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        Hash hashB = transactionB.getHash();
        pool.addOrphanTransaction(transactionB);

        Transaction transactionC = new Transaction(
            Collections.singletonList(new Input(Simulation.randomSignature(), hashB, 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        Hash hashC = transactionC.getHash();
        pool.addOrphanTransaction(transactionC);

        Hash parent = transactionA.getInputs().get(0).getReferencedTransaction();

        List<Transaction> removed = pool.removeValidOrphansFromParent(parent, t -> !t.getHash().equals(hashC));

        assertEquals(2, removed.size());

        assertFalse(pool.isOrphan(hashA));
        assertFalse(pool.isOrphan(hashB));
        assertTrue(pool.isOrphan(hashC));
    }

    @Test
    void promoteDependentToIndependentFromParent() {
        Transaction transactionA = Simulation.randomTransaction(5, 5);
        Hash hashA = transactionA.getHash();
        pool.addDependentTransaction(transactionA);

        Transaction transactionB = new Transaction(
            Collections.singletonList(new Input(Simulation.randomSignature(), hashA, 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        Hash hashB = transactionB.getHash();
        pool.addDependentTransaction(transactionB);

        Transaction transactionC = new Transaction(
            Collections.singletonList(new Input(Simulation.randomSignature(), hashB, 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        Hash hashC = transactionC.getHash();
        pool.addDependentTransaction(transactionC);

        Hash parent = transactionA.getInputs().get(0).getReferencedTransaction();

        pool.promoteDependentToIndependentFromParent(parent, t -> !t.getHash().equals(hashC));

        assertTrue(pool.isIndependent(hashA));
        assertTrue(pool.isIndependent(hashB));
        assertFalse(pool.isIndependent(hashC));

        assertFalse(pool.isDependent(hashA));
        assertFalse(pool.isDependent(hashB));
        assertTrue(pool.isDependent(hashC));
    }

    @Test
    void demoteIndependentToDependent() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        pool.addIndependentTransaction(transaction);

        Hash dependency = transaction.getInputs().get(0).getReferencedTransaction();

        pool.demoteIndependentToDependent(dependency);

        assertTrue(pool.isDependent(hash));
        assertFalse(pool.isIndependent(hash));
    }

    @Test
    void hasValidTransactionIndependent() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        pool.addIndependentTransaction(transaction);

        assertTrue(pool.hasValidTransaction(hash));
    }

    @Test
    void hasValidTransactionDependent() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        pool.addIndependentTransaction(transaction);

        assertTrue(pool.hasValidTransaction(hash));
    }

    @Test
    void hasValidTransactionNonexistent() {
        assertFalse(pool.hasValidTransaction(Simulation.randomHash()));
    }

    @Test
    void hasOrphan() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        pool.addOrphanTransaction(transaction);

        assertTrue(pool.isOrphan(hash));
    }

    @Test
    void hasOrphanNonexistent() {
        assertFalse(pool.isOrphan(Simulation.randomHash()));
    }

    @Test
    void limitTransactionPoolSizeChildOnly() {
        BraboConfig newConfig = new MockBraboConfig(config) {
            @Override
            public int maxTransactionPoolSize() {
                return 2;
            }
        };

        Random mockRandom = new Random() {
            @Override
            public int nextInt(int bound) {
                return 1;  // Random enough
            }
        };

        pool = new TransactionPool(newConfig.maxTransactionPoolSize(), newConfig.maxOrphanTransactions(), mockRandom);

        Transaction transactionA = Simulation.randomTransaction(5, 5);
        Hash hashA = transactionA.getHash();

        Transaction transactionB = Simulation.randomTransaction(5, 5);
        Hash hashB = transactionB.getHash();

        Transaction transactionC = new Transaction(
            Collections.singletonList(new Input(Simulation.randomSignature(), hashB, 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        Hash hashC = transactionC.getHash();

        pool.addIndependentTransaction(transactionA);
        pool.addDependentTransaction(transactionB);
        pool.addDependentTransaction(transactionC);

        pool.limitTransactionPoolSize();

        // Assert the dependent transaction is removed first
        assertFalse(pool.isDependent(hashC));
        assertTrue(pool.isDependent(hashB));
        assertTrue(pool.isIndependent(hashA));
    }

    @Test
    void limitTransactionPoolSizeParentFirst() {
        BraboConfig newConfig = new MockBraboConfig(config) {
            @Override
            public int maxTransactionPoolSize() {
                return 1;
            }
        };

        Random mockRandom = new Random() {
            @Override
            public int nextInt(int bound) {
                return 0;  // Random enough
            }
        };

        pool = new TransactionPool(newConfig.maxTransactionPoolSize(), newConfig.maxOrphanTransactions(), mockRandom);

        Transaction transactionA = Simulation.randomTransaction(5, 5);
        Hash hashA = transactionA.getHash();

        Transaction transactionB = Simulation.randomTransaction(5, 5);
        Hash hashB = transactionB.getHash();

        Transaction transactionC = new Transaction(
            Collections.singletonList(new Input(Simulation.randomSignature(), hashB, 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        Hash hashC = transactionC.getHash();

        pool.addIndependentTransaction(transactionA);
        pool.addDependentTransaction(transactionB);
        pool.addDependentTransaction(transactionC);

        pool.limitTransactionPoolSize();

        // Assert the dependent transaction is removed first
        assertFalse(pool.isDependent(hashB));
        assertFalse(pool.isDependent(hashC));
        assertTrue(pool.isIndependent(hashA));
    }

    @Test
    void limitTransactionPoolSizeIndependent() {
        BraboConfig newConfig = new MockBraboConfig(config) {
            @Override
            public int maxTransactionPoolSize() {
                return 1;
            }
        };

        Random mockRandom = new Random() {
            @Override
            public int nextInt(int bound) {
                return 0;  // Random enough
            }
        };

        pool = new TransactionPool(newConfig.maxTransactionPoolSize(), newConfig.maxOrphanTransactions(), mockRandom);

        Transaction transactionA = Simulation.randomTransaction(5, 5);
        Hash hashA = transactionA.getHash();

        Transaction transactionB = Simulation.randomTransaction(5, 5);
        Hash hashB = transactionB.getHash();

        Transaction transactionC = Simulation.randomTransaction(5, 5);
        Hash hashC = transactionC.getHash();

        pool.addIndependentTransaction(transactionA);
        pool.addIndependentTransaction(transactionB);
        pool.addDependentTransaction(transactionC);

        pool.limitTransactionPoolSize();

        // Assert the dependent transaction is removed first
        assertFalse(pool.isIndependent(hashA));
        assertTrue(pool.isIndependent(hashB));
        assertFalse(pool.isDependent(hashC));
    }

    @Test
    void maxOrphanTransactions() {
        BraboConfig newConfig = new MockBraboConfig(config) {
            @Override
            public int maxOrphanTransactions() {
                return 2;
            }
        };

        Random mockRandom = new Random() {
            @Override
            public int nextInt(int bound) {
                return 0;  // Random enough
            }
        };

        pool = new TransactionPool(newConfig.maxTransactionPoolSize(), newConfig.maxOrphanTransactions(), mockRandom);

        Transaction transactionA = Simulation.randomTransaction(5, 5);
        Hash hashA = transactionA.getHash();

        Transaction transactionB = Simulation.randomTransaction(5, 5);
        Hash hashB = transactionB.getHash();

        Transaction transactionC = Simulation.randomTransaction(5, 5);
        Hash hashC = transactionC.getHash();

        pool.addOrphanTransaction(transactionA);
        pool.addOrphanTransaction(transactionB);
        pool.addOrphanTransaction(transactionC);

        // Assert the dependent transaction is removed first
        assertFalse(pool.isOrphan(hashA));
        assertTrue(pool.isOrphan(hashB));
        assertTrue(pool.isOrphan(hashC));
    }
}
