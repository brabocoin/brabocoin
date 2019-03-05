package org.brabocoin.brabocoin.config;

import java.util.List;

/**
 * The configuration class for Brabocoin
 */
public interface BraboConfig {
    int getNetworkId();

    int getServicePort();

    int getTargetPeerCount();

    int getUpdatePeerInterval();

    boolean isAllowLocalPeers();

    int getMaxSequentialOrphanBlocks();

    int getLoopInterval();

    int getHandshakeDeadline();

    public List<String> getBootstrapPeers();

    String getDataDirectory();

    String getDatabaseDirectory();

    String getBlockStoreDirectory();

    String getUtxoStoreDirectory();

    String getWalletStoreDirectory();

    String getWalletFile();

    String getTransactionHistoryFile();

    int getMaxBlockFileSize();

    int getMaxOrphanBlocks();

    int getMaxRecentRejectBlocks();

    int getMaxTransactionPoolSize();

    int getMaxOrphanTransactions();

    int getMaxRecentRejectTransactions();
}
