package org.brabocoin.brabocoin.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BraboConfigAdapter implements BraboConfig {

    private final int networkId;
    private final int servicePort;
    private final int targetPeerCount;
    private final int updatePeerInterval;
    private final boolean allowLocalPeers;
    private final int maxSequentialOrphanBlocks;
    private final int loopInterval;
    private final int handshakeDeadline;
    private final List<String> bootstrapPeers;
    private final String dataDirectory;
    private final String databaseDirectory;
    private final String blockStoreDirectory;
    private final String utxoStoreDirectory;
    private final String walletStoreDirectory;
    private final String walletFile;
    private final String transactionHistoryFile;
    private final int maxBlockFileSize;
    private final int maxOrphanBlocks;
    private final int maxRecentRejectBlocks;
    private final int maxTransactionPoolSize;
    private final int maxOrphanTransactions;
    private final int maxRecentRejectTransactions;

    public BraboConfigAdapter(MutableBraboConfig mutableBraboConfig) {
        networkId = mutableBraboConfig.getNetworkId();
        servicePort = mutableBraboConfig.getServicePort();
        targetPeerCount = mutableBraboConfig.getTargetPeerCount();
        updatePeerInterval = mutableBraboConfig.getUpdatePeerInterval();
        allowLocalPeers = mutableBraboConfig.isAllowLocalPeers();
        maxSequentialOrphanBlocks = mutableBraboConfig.getMaxSequentialOrphanBlocks();
        loopInterval = mutableBraboConfig.getLoopInterval();
        handshakeDeadline = mutableBraboConfig.getHandshakeDeadline();
        bootstrapPeers = new ArrayList<>(mutableBraboConfig.getBootstrapPeers());
        dataDirectory = mutableBraboConfig.getDataDirectory();
        databaseDirectory = mutableBraboConfig.getDatabaseDirectory();
        blockStoreDirectory = mutableBraboConfig.getBlockStoreDirectory();
        utxoStoreDirectory = mutableBraboConfig.getUtxoStoreDirectory();
        walletStoreDirectory = mutableBraboConfig.getWalletStoreDirectory();
        walletFile = mutableBraboConfig.getWalletFile();
        transactionHistoryFile = mutableBraboConfig.getTransactionHistoryFile();
        maxBlockFileSize = mutableBraboConfig.getMaxBlockFileSize();
        maxOrphanBlocks = mutableBraboConfig.getMaxOrphanBlocks();
        maxRecentRejectBlocks = mutableBraboConfig.getMaxRecentRejectBlocks();
        maxTransactionPoolSize = mutableBraboConfig.getMaxTransactionPoolSize();
        maxOrphanTransactions = mutableBraboConfig.getMaxOrphanTransactions();
        maxRecentRejectTransactions = mutableBraboConfig.getMaxRecentRejectTransactions();
    }

    @Override
    public int getNetworkId() {
        return networkId;
    }

    @Override
    public int getServicePort() {
        return servicePort;
    }

    @Override
    public int getTargetPeerCount() {
        return targetPeerCount;
    }

    @Override
    public int getUpdatePeerInterval() {
        return updatePeerInterval;
    }

    @Override
    public boolean isAllowLocalPeers() {
        return false;
    }

    @Override
    public int getMaxSequentialOrphanBlocks() {
        return maxSequentialOrphanBlocks;
    }

    @Override
    public int getLoopInterval() {
        return loopInterval;
    }

    @Override
    public int getHandshakeDeadline() {
        return handshakeDeadline;
    }

    @Override
    public List<String> getBootstrapPeers() {
        return Collections.unmodifiableList(bootstrapPeers);
    }

    @Override
    public String getDataDirectory() {
        return dataDirectory;
    }

    @Override
    public String getDatabaseDirectory() {
        return databaseDirectory;
    }

    @Override
    public String getBlockStoreDirectory() {
        return blockStoreDirectory;
    }

    @Override
    public String getUtxoStoreDirectory() {
        return utxoStoreDirectory;
    }

    @Override
    public String getWalletStoreDirectory() {
        return walletStoreDirectory;
    }

    @Override
    public String getWalletFile() {
        return walletFile;
    }

    @Override
    public String getTransactionHistoryFile() {
        return transactionHistoryFile;
    }

    @Override
    public int getMaxBlockFileSize() {
        return maxBlockFileSize;
    }

    @Override
    public int getMaxOrphanBlocks() {
        return maxOrphanBlocks;
    }

    @Override
    public int getMaxRecentRejectBlocks() {
        return maxRecentRejectBlocks;
    }

    @Override
    public int getMaxTransactionPoolSize() {
        return maxTransactionPoolSize;
    }

    @Override
    public int getMaxOrphanTransactions() {
        return maxOrphanTransactions;
    }

    @Override
    public int getMaxRecentRejectTransactions() {
        return maxRecentRejectTransactions;
    }
}
