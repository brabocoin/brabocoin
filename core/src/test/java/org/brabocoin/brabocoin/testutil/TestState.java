package org.brabocoin.brabocoin.testutil;

import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.state.DeploymentState;
import org.brabocoin.brabocoin.util.Destructible;
import org.jetbrains.annotations.NotNull;


/**
 * Test state environment.
 */
public class TestState extends DeploymentState {

    public TestState(@NotNull BraboConfig config) throws DatabaseException {
        super(config, (c) -> new Destructible<>("testpassphrase"::toCharArray));
    }

    @Override
    protected KeyValueStore createBlockStorage() {
        return new HashMapDB();
    }

    @Override
    protected KeyValueStore createUtxoStorage() {
        return new HashMapDB();
    }

    @Override
    protected KeyValueStore createWalletUTXOStorage() {
        return new HashMapDB();
    }
}
