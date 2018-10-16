package org.brabocoin.brabocoin.testutil;

import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.config.BraboConfig;

import java.util.List;
import java.util.Map;

public class MockEnvironment extends NodeEnvironment {
    public MockEnvironment(BraboConfig mockConfig) throws DatabaseException {
        super();
        this.config = mockConfig;
    }

    public MockEnvironment(BraboConfig mockConfig, BlockDatabase database) throws DatabaseException {
        super(database);
        this.config = mockConfig;
    }

    public MockEnvironment(BraboConfig mockConfig, Map<Hash, Transaction> transactions) throws DatabaseException {
        super(transactions);
        this.config = mockConfig;
    }

    @Override
    protected KeyValueStore createKeyValueStorage() {
        return new HashMapDB();
    }
}