package org.brabocoin.brabocoin.testutil;

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
    public MockEnvironment(KeyValueStore store, BraboConfig config, List<Block> initialBlocks) throws DatabaseException {
        super(store, config);
        for (Block b : initialBlocks) {
            database.storeBlock(b, true);
        }
    }
}