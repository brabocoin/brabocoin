package org.brabocoin.brabocoin.dal;

import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Signature;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

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
        pool = new TransactionPool(config);
    }

    @Test
    void addFindIndependentTransaction() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.computeHash();
        pool.addIndependentTransaction(transaction);

        Transaction fromPool = pool.findValidatedTransaction(hash);
        assertNotNull(fromPool);
        assertEquals(hash, fromPool.computeHash());
    }

    @Test
    void addFindDependentTransaction() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.computeHash();
        pool.addDependentTransaction(transaction);

        Transaction fromPool = pool.findValidatedTransaction(hash);
        assertNotNull(fromPool);
        assertEquals(hash, fromPool.computeHash());
    }

    @Test
    void addFindOrphan() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.computeHash();
        pool.addOrphanTransaction(transaction);

        Transaction fromPool = pool.findOrphan(hash);
        assertNotNull(fromPool);
        assertEquals(hash, fromPool.computeHash());
    }

    @Test
    void orphanIsNotValidated() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.computeHash();
        pool.addOrphanTransaction(transaction);

        assertNull(pool.findValidatedTransaction(hash));
    }

    @Test
    void independentIsNotOrphan() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.computeHash();
        pool.addIndependentTransaction(transaction);

        assertNull(pool.findOrphan(hash));
    }

    @Test
    void dependentIsNotOrphan() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.computeHash();
        pool.addDependentTransaction(transaction);

        assertNull(pool.findOrphan(hash));
    }

    @Test
    void removeValidatedTransactionIndependent() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.computeHash();
        pool.addIndependentTransaction(transaction);

        pool.removeValidatedTransaction(hash);

        assertFalse(pool.hasValidTransaction(hash));
    }

    @Test
    void removeValidatedTransactionDependent() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.computeHash();
        pool.addDependentTransaction(transaction);

        pool.removeValidatedTransaction(hash);

        assertFalse(pool.hasValidTransaction(hash));
    }

    @Test
    void isDependent() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.computeHash();
        pool.addDependentTransaction(transaction);

        assertTrue(pool.isDependent(hash));
    }

    @Test
    void isIndependent() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.computeHash();
        pool.addIndependentTransaction(transaction);

        assertTrue(pool.isIndependent(hash));
    }

    @Test
    void removeValidOrphansFromParent() {
        Transaction transactionA = Simulation.randomTransaction(5, 5);
        Hash hashA = transactionA.computeHash();
        pool.addOrphanTransaction(transactionA);

        Transaction transactionB = new Transaction(
            Collections.singletonList(new Input(new Signature(), hashA, 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        Hash hashB = transactionB.computeHash();
        pool.addOrphanTransaction(transactionB);

        Transaction transactionC = new Transaction(
            Collections.singletonList(new Input(new Signature(), hashB, 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        Hash hashC = transactionC.computeHash();
        pool.addOrphanTransaction(transactionC);

        Hash parent = transactionA.getInputs().get(0).getReferencedTransaction();

        List<Transaction> removed = pool.removeValidOrphansFromParent(parent, t -> !t.computeHash().equals(hashC));

        assertEquals(2, removed.size());

        assertFalse(pool.isOrphan(hashA));
        assertFalse(pool.isOrphan(hashB));
        assertTrue(pool.isOrphan(hashC));
    }

    @Test
    void promoteDependentToIndependentFromParent() {
        Transaction transactionA = Simulation.randomTransaction(5, 5);
        Hash hashA = transactionA.computeHash();
        pool.addDependentTransaction(transactionA);

        Transaction transactionB = new Transaction(
            Collections.singletonList(new Input(new Signature(), hashA, 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        Hash hashB = transactionB.computeHash();
        pool.addDependentTransaction(transactionB);

        Transaction transactionC = new Transaction(
            Collections.singletonList(new Input(new Signature(), hashB, 0)),
            Collections.singletonList(Simulation.randomOutput())
        );
        Hash hashC = transactionC.computeHash();
        pool.addDependentTransaction(transactionC);

        Hash parent = transactionA.getInputs().get(0).getReferencedTransaction();

        pool.promoteDependentToIndependentFromParent(parent, t -> !t.computeHash().equals(hashC));

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
        Hash hash = transaction.computeHash();
        pool.addIndependentTransaction(transaction);

        Hash dependency = transaction.getInputs().get(0).getReferencedTransaction();

        pool.demoteIndependentToDependent(dependency);

        assertTrue(pool.isDependent(hash));
        assertFalse(pool.isIndependent(hash));
    }

    @Test
    void hasValidTransactionIndependent() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.computeHash();
        pool.addIndependentTransaction(transaction);

        assertTrue(pool.hasValidTransaction(hash));
    }

    @Test
    void hasValidTransactionDependent() {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.computeHash();
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
        Hash hash = transaction.computeHash();
        pool.addOrphanTransaction(transaction);

        assertTrue(pool.isOrphan(hash));
    }

    @Test
    void hasOrphanNonexistent() {
        assertFalse(pool.isOrphan(Simulation.randomHash()));
    }
}
