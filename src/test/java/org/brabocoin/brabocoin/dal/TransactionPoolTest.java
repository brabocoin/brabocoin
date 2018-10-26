package org.brabocoin.brabocoin.dal;

import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
