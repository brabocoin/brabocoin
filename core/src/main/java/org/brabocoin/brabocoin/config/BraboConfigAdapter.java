package org.brabocoin.brabocoin.config;

import org.brabocoin.brabocoin.model.Hash;

import java.util.List;

/**
 * Wraps a {@link BraboConfig} in another {@link BraboConfig}, delegating every call.
 */
public class BraboConfigAdapter implements BraboConfig {

    private BraboConfig delegator;

    public void setDelegator(BraboConfig delegator) {
        this.delegator = delegator;
    }

    public BraboConfigAdapter(BraboConfig delegator) {
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
        return delegator.dataDirectory();
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
    public Hash targetValue() {
        return delegator.targetValue();
    }

    @Override
    public Boolean allowLocalPeers() {
        return delegator.allowLocalPeers();
    }
}
