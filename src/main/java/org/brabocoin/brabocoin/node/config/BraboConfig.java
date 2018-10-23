package org.brabocoin.brabocoin.node.config;

import java.util.List;

public interface BraboConfig {

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
    long maxBlockFileSize();
}
