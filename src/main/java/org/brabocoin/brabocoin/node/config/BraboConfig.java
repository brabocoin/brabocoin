package org.brabocoin.brabocoin.node.config;

import java.util.List;

public interface BraboConfig {
    int targetPeerCount();
    int bootstrapDeadline();
    List<String> bootstrapPeers();
    String databaseDirectory();
    String blockStoreDirectory();
    String utxoStoreDirectory();
}