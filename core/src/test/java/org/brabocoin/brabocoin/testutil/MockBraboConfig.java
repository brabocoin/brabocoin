package org.brabocoin.brabocoin.testutil;

import org.brabocoin.brabocoin.node.config.BraboConfig;

import java.util.List;

public class MockBraboConfig implements BraboConfig {

    BraboConfig delegator;

    public MockBraboConfig(BraboConfig delegator) {
        this.delegator = delegator;
    }

    @Override
    public int networkId() {
        return this.delegator.networkId();
    }

    @Override
    public int loopInterval() {
        return delegator.loopInterval();
    }

    @Override
    public int targetPeerCount() {
        return delegator.targetPeerCount();
    }

    @Override
    public int bootstrapDeadline() {
        return delegator.bootstrapDeadline();
    }

    @Override
    public List<String> bootstrapPeers() {
        return delegator.bootstrapPeers();
    }

    @Override
    public String databaseDirectory() {
        return delegator.databaseDirectory();
    }

    @Override
    public String blockStoreDirectory() {
        return delegator.blockStoreDirectory();
    }

    @Override
    public String utxoStoreDirectory() {
        return delegator.utxoStoreDirectory();
    }

    @Override
    public int maxBlockFileSize() {
        return delegator.maxBlockFileSize();
    }

    @Override
    public int maxTransactionPoolSize() {
        return delegator.maxTransactionPoolSize();
    }

    @Override
    public int maxOrphanTransactions() {
        return delegator.maxOrphanTransactions();
    }

    @Override
    public int maxOrphanBlocks() {
        return delegator.maxOrphanBlocks();
    }

    @Override
    public int servicePort() {
        return delegator.servicePort();
    }

    @Override
    public String walletFile() {
        return delegator.walletFile();
    }
}
