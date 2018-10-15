package org.brabocoin.brabocoin.testutil;

import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.config.BraboConfig;

import java.util.List;

public class MockEnvironment extends NodeEnvironment {
    public MockEnvironment(BraboConfig mockConfig) throws DatabaseException {
        super();
        this.config = mockConfig;
    }

    public MockEnvironment(BraboConfig mockConfig, List<Block> initialBlocks) throws DatabaseException {
        this(mockConfig);
        for (Block b : initialBlocks) {
            database.storeBlock(b, true);
        }
    }

    @Override
    protected KeyValueStore getKeyValueStorage() {
        return new HashMapDB();
    }
}