package org.brabocoin.brabocoin.testutil;

import java.util.List;

public class MockLegacyConfig extends LegacyBraboConfig {

    private LegacyBraboConfig delegator;

    public void setDelegator(LegacyBraboConfig delegator) {
        this.delegator = delegator;
    }

    public MockLegacyConfig(LegacyBraboConfig delegator) {
        super(null);
        this.delegator = delegator;
    }

    public MockLegacyConfig(MockLegacyConfig delegator) {
        super(null);
        this.delegator = delegator;
    }

    @Override
    public Integer networkId() {
        return this.delegator.networkId();
    }


    @Override
    public Integer loopInterval() {
        return delegator.loopInterval();
    }


    @Override
    public Integer targetPeerCount() {
        return delegator.targetPeerCount();
    }


    @Override
    public Integer handshakeDeadline() {
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
    public Integer updatePeerInterval() {
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
    public Integer maxBlockFileSize() {
        return delegator.maxBlockFileSize();
    }


    @Override
    public Integer maxTransactionPoolSize() {
        return delegator.maxTransactionPoolSize();
    }


    @Override
    public Integer maxOrphanTransactions() {
        return delegator.maxOrphanTransactions();
    }


    @Override
    public Integer maxOrphanBlocks() {
        return delegator.maxOrphanBlocks();
    }


    @Override
    public Integer maxRecentRejectBlocks() {
        return delegator.maxRecentRejectBlocks();
    }


    @Override
    public Integer maxRecentRejectTransactions() {
        return delegator.maxRecentRejectTransactions();
    }


    @Override
    public Integer servicePort() {
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
    public Integer maxSequentialOrphanBlocks() {
        return delegator.maxSequentialOrphanBlocks();
    }


    @Override
    public Boolean allowLocalPeers() {
        return delegator.allowLocalPeers();
    }
}
