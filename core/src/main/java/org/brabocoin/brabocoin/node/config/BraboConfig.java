package org.brabocoin.brabocoin.node.config;

import java.util.List;

public interface BraboConfig {

    int networkId();

    int loopInterval();

    int targetPeerCount();

    int bootstrapDeadline();

    List<String> bootstrapPeers();

    String databaseDirectory();

    String blockStoreDirectory();

    String utxoStoreDirectory();

    /**
     * Maximim file size of the block storage files in bytes.
     *
     * @return The maximum file size.
     */
    int maxBlockFileSize();

    /**
     * Maximum size of the transaction pool in number of transactions.
     *
     * @return The maximum transaction pool size.
     */
    int maxTransactionPoolSize();

    /**
     * Maximum number of orphan transactions kept in memory.
     *
     * @return The maximum number of orphan transactions.
     */
    int maxOrphanTransactions();

    /**
     * Port on which the application listens for messages on the network.
     *
     * @return The port number.
     */
    int servicePort();

    /**
     * File in which the wallet is stored.
     *
     * @return The wallet file path.
     */
    String walletFile();
}
