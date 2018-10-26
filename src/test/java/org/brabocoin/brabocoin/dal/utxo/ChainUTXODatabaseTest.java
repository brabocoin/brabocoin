package org.brabocoin.brabocoin.dal.utxo;

import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.validation.Consensus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UTXO Database tests.
 */
class ChainUTXODatabaseTest {

    private KeyValueStore storage;
    private Consensus consensus;
    private ChainUTXODatabase database;

    @BeforeEach
    void setUp() throws DatabaseException {
        storage = new HashMapDB();
        consensus = new Consensus();
        database = new ChainUTXODatabase(storage, consensus);
    }

    @Test
    void genesisProcessedBlockHash() throws DatabaseException {
        assertEquals(consensus.getGenesisBlock().computeHash(), database.getLastProcessedBlockHash());
    }

    @Test
    void lastProcessedBlockHash() throws DatabaseException {
        Hash hash = Simulation.randomHash();

        database.setLastProcessedBlockHash(hash);
        Hash storedHash = database.getLastProcessedBlockHash();

        assertEquals(hash, storedHash);
    }
}
