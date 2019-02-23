package org.brabocoin.brabocoin.config;

import org.brabocoin.brabocoin.config.annotation.BraboPref;
import org.brabocoin.brabocoin.model.Hash;

import java.util.List;

public interface BraboConfig {
    String braboConfigSection = "brabo";

    @BraboPref(name = "Network ID", group = BraboPreferencesTree.TopLevel.GroupLevel1.class)
    Integer networkId();


    @BraboPref(name = "Loop interval", group =
        BraboPreferencesTree.CategoryLevel1.GroupLevel3.class)
    Integer loopInterval();

    Integer targetPeerCount();

    Integer handshakeDeadline();

    List<String> bootstrapPeers();

    String dataDirectory();

    Integer updatePeerInterval();

    String databaseDirectory();

    String blockStoreDirectory();

    String utxoStoreDirectory();

    String walletStoreDirectory();

    /**
     * Maximum file size of the block storage files in bytes.
     *
     * @return The maximum file size.
     */
    Integer maxBlockFileSize();

    /**
     * Maximum size of the transaction pool in number of transactions.
     *
     * @return The maximum transaction pool size.
     */
    Integer maxTransactionPoolSize();

    /**
     * Maximum number of orphan transactions kept in memory.
     *
     * @return The maximum number of orphan transactions.
     */
    Integer maxOrphanTransactions();

    /**
     * Maximum number of orphan blocks kept in memory.
     *
     * @return The maximum number of orphan blocks.
     */
    Integer maxOrphanBlocks();

    /**
     * Maximum number of recently rejected blocks in memory.
     *
     * @return The maximum number of recently rejected blocks.
     */
    Integer maxRecentRejectBlocks();

    /**
     * Maximum number of recently rejected transactions in memory.
     *
     * @return The maximum number of recently rejected transactions.
     */
    Integer maxRecentRejectTransactions();

    /**
     * Port on which the application listens for messages on the network.
     *
     * @return The port number.
     */
    Integer servicePort();

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

    Integer maxSequentialOrphanBlocks();

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
    Boolean allowLocalPeers();
}
