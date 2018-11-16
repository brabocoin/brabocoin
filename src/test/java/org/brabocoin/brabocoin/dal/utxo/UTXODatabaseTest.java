package org.brabocoin.brabocoin.dal.utxo;

import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.dal.UTXODatabase;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test {@link org.brabocoin.brabocoin.dal.UTXODatabase}.
 */
public class UTXODatabaseTest {

    protected UTXODatabase database;
    protected KeyValueStore storage;

    @BeforeEach
    void setUp() {
        storage = new HashMapDB();
        database = new UTXODatabase(storage);
    }

    @Test
    void transactionUnspentByIndex() throws DatabaseException {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        database.setOutputsUnspent(transaction, 0);

        for (int i = 0; i < transaction.getOutputs().size(); i++) {
            assertTrue(database.isUnspent(hash, i));
        }
    }

    @Test
    void transactionUnspentByInput() throws DatabaseException {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        database.setOutputsUnspent(transaction, 0);

        for (int i = 0; i < transaction.getOutputs().size(); i++) {
            Input input = new Input(Simulation.randomSignature(), hash, i);
            assertTrue(database.isUnspent(input));
        }
    }

    @Test
    void transactionUnspentOnlyFirst() throws DatabaseException {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        database.setOutputsUnspent(transaction, Collections.singletonList(0), 0);

        assertTrue(database.isUnspent(hash, 0));
    }

    @Test
    void markUnspentSpent() throws DatabaseException {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        database.setOutputsUnspent(transaction, Collections.singletonList(0), 0);

        database.setOutputSpent(hash, 0);

        assertFalse(database.isUnspent(hash, 0));
    }

    @Test
    void findUnspentOutputInfo() throws DatabaseException {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        database.setOutputsUnspent(transaction, Collections.singletonList(0), 0);

        UnspentOutputInfo info = database.findUnspentOutputInfo(hash, 0);

        assertNotNull(info);
    }

    @Test
    void findUnspentOutputInfoByInput() throws DatabaseException {
        Transaction transaction = Simulation.randomTransaction(5, 5);
        Hash hash = transaction.getHash();
        database.setOutputsUnspent(transaction, Collections.singletonList(0), 0);

        Input input = new Input(Simulation.randomSignature(), hash, 0);
        UnspentOutputInfo info = database.findUnspentOutputInfo(input);

        assertNotNull(info);
    }

    @Test
    void nonExistingIsSpent() throws DatabaseException {
        assertFalse(database.isUnspent(Simulation.randomHash(), 0));
    }

    @Test
    void nonExistingUnspentOutputInfo() throws DatabaseException {
        assertNull(database.findUnspentOutputInfo(Simulation.randomHash(), 0));
    }
}
