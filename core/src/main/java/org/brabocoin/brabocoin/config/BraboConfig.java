package org.brabocoin.brabocoin.config;

import org.brabocoin.brabocoin.config.annotations.BraboPref;
import org.brabocoin.brabocoin.model.Hash;

import java.util.List;

public interface BraboConfig {

    @BraboPref(group = BraboPreferencesTree.Test.TestGroup1.class)
    int networkId();

    int loopInterval();

    int targetPeerCount();

    int handshakeDeadline();

    List<String> bootstrapPeers();

    String dataDirectory();

    int updatePeerInterval();

    String databaseDirectory();

    String blockStoreDirectory();

    String utxoStoreDirectory();

    String walletStoreDirectory();

    /**
     * Maximum file size of the block storage files in bytes.
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
     * Maximum number of orphan blocks kept in memory.
     *
     * @return The maximum number of orphan blocks.
     */
    int maxOrphanBlocks();

    /**
     * Maximum number of recently rejected blocks in memory.
     *
     * @return The maximum number of recently rejected blocks.
     */
    int maxRecentRejectBlocks();

    /**
     * Maximum number of recently rejected transactions in memory.
     *
     * @return The maximum number of recently rejected transactions.
     */
    int maxRecentRejectTransactions();

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

    /**
     * File in which the wallet transaction history is stored.
     *
     * @return The transaction history file path.
     */
    String transactionHistoryFile();

    int maxSequentialOrphanBlocks();

    /**
     * The target value for blocks.
     * <p>
     * The representation of the target value is interpreted as:
     * <ul>
     * <li>If the string starts with a zero, the string is interpreted as the hexadeximal hash
     * representation of the target value.</li>
     * <li>Otherwise, the string is parsed as a BigDecimal value in base 10, and then converted
     * to a BigInteger (allowing scientific notation).</li>
     * </ul>
     *
     * @return The block target value.
     */
    Hash targetValue();

    /**
     * Whether or not to allow local peers.
     */
    boolean allowLocalPeers();
}
