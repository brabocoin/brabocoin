package org.brabocoin.brabocoin.testutil;

import org.brabocoin.brabocoin.model.Hash;
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
    public int handshakeDeadline() {
        return delegator.handshakeDeadline();
    }

    @Override
    public List<String> bootstrapPeers() {
        return delegator.bootstrapPeers();
    }

    @Override
    public String dataDirectory() {
        return "src/test/resources/" + delegator.dataDirectory();
    }

    @Override
    public int updatePeerInterval() {
        return delegator.updatePeerInterval();
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
    public String walletStoreDirectory() {
        return delegator.walletStoreDirectory();
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
    public int maxRecentRejectBlocks() {
        return delegator.maxRecentRejectBlocks();
    }

    @Override
    public int maxRecentRejectTransactions() {
        return delegator.maxRecentRejectTransactions();
    }

    @Override
    public int servicePort() {
        return delegator.servicePort();
    }

    @Override
    public String walletFile() {
        return delegator.walletFile();
    }

    @Override
    public String transactionHistoryFile() {
        return delegator.transactionHistoryFile();
    }

    @Override
    public int maxSequentialOrphanBlocks() {
        return delegator.maxSequentialOrphanBlocks();
    }

    @Override
    public Hash targetValue() {
        return delegator.targetValue();
    }
}
