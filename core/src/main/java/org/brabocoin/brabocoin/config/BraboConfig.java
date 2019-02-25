package org.brabocoin.brabocoin.config;

import org.brabocoin.brabocoin.config.annotation.BraboPref;
import org.brabocoin.brabocoin.model.Hash;

import java.util.List;

public interface BraboConfig {
    String braboConfigSection = "brabo";

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
    @BraboPref(name = "Target value", destination = BraboPreferencesTree.Consensus.class, order = 0)
    Hash targetValue();

    @BraboPref(name = "Network ID", destination = BraboPreferencesTree.NetworkCategory.General.class, order = 0)
    Integer networkId();

    @BraboPref(name = "Service port number", destination = BraboPreferencesTree.NetworkCategory.General.class, order = 1)
    Integer servicePort();

    @BraboPref(name = "Target peer count", destination = BraboPreferencesTree.NetworkCategory.General.class, order = 2)
    Integer targetPeerCount();

    @BraboPref(name = "Update peer interval (ms)", destination = BraboPreferencesTree.NetworkCategory.General.class, order = 3)
    Integer updatePeerInterval();

    @BraboPref(name = "Allow local peers", destination = BraboPreferencesTree.NetworkCategory.General.class, order = 4)
    Boolean allowLocalPeers();

    @BraboPref(name = "Number of orphan blocks before syncing", destination = BraboPreferencesTree.NetworkCategory.Advanced.class, order = 0)
    Integer maxSequentialOrphanBlocks();

    @BraboPref(name = "Message processing interval (ms)", destination = BraboPreferencesTree.NetworkCategory.Advanced.class, order = 1)
    Integer loopInterval();

    @BraboPref(name = "Handshake response deadline (ms)", destination = BraboPreferencesTree.NetworkCategory.Advanced.class, order = 2)
    Integer handshakeDeadline();

    // Note: lists do not have a PreferencesFX control
    List<String> bootstrapPeers();

    @BraboPref(name = "Data directory", destination = BraboPreferencesTree.StorageCategory.General.class, order = 0)
    String dataDirectory();

    @BraboPref(name = "Database subdirectory", destination = BraboPreferencesTree.StorageCategory.General.class, order = 1)
    String databaseDirectory();

    @BraboPref(name = "Block storage subdirectory", destination = BraboPreferencesTree.StorageCategory.General.class, order = 2)
    String blockStoreDirectory();

    @BraboPref(name = "UTXO storage subdirectory", destination = BraboPreferencesTree.StorageCategory.General.class, order = 3)
    String utxoStoreDirectory();

    @BraboPref(name = "Wallet storage subdirectory", destination = BraboPreferencesTree.StorageCategory.General.class, order = 4)
    String walletStoreDirectory();

    /**
     * Maximum file size of the block storage files in bytes.
     *
     * @return The maximum file size.
     */
    @BraboPref(name = "Maximum block storage file size (bytes)", destination = BraboPreferencesTree.StorageCategory.Block.class, order = 0)
    Integer maxBlockFileSize();

    /**
     * Maximum number of orphan blocks kept in memory.
     *
     * @return The maximum number of orphan blocks.
     */
    @BraboPref(name = "Maximum orphan blocks in memory", destination = BraboPreferencesTree.StorageCategory.Block.class, order = 1)
    Integer maxOrphanBlocks();

    /**
     * Maximum number of recently rejected blocks in memory.
     *
     * @return The maximum number of recently rejected blocks.
     */
    @BraboPref(name = "Maximum rejected blocks in memory", destination = BraboPreferencesTree.StorageCategory.Block.class, order = 2)
    Integer maxRecentRejectBlocks();

    /**
     * Maximum size of the transaction pool in number of transactions.
     *
     * @return The maximum transaction pool size.
     */
    @BraboPref(name = "Maximum transactions in pool", destination = BraboPreferencesTree.StorageCategory.Transaction.class, order = 0)
    Integer maxTransactionPoolSize();

    /**
     * Maximum number of orphan transactions kept in memory.
     *
     * @return The maximum number of orphan transactions.
     */
    @BraboPref(name = "Maximum orphan transactions in memory", destination = BraboPreferencesTree.StorageCategory.Transaction.class, order = 1)
    Integer maxOrphanTransactions();

    /**
     * Maximum number of recently rejected transactions in memory.
     *
     * @return The maximum number of recently rejected transactions.
     */
    @BraboPref(name = "Maximum rejected transactions in memory", destination = BraboPreferencesTree.StorageCategory.Transaction.class, order = 2)
    Integer maxRecentRejectTransactions();

    /**
     * File in which the wallet is stored.
     *
     * @return The wallet file path.
     */

    @BraboPref(name = "Wallet file", destination = BraboPreferencesTree.StorageCategory.Wallet.class, order = 0)
    String walletFile();

    /**
     * File in which the wallet transaction history is stored.
     *
     * @return The transaction history file path.
     */
    @BraboPref(name = "Transaction history file", destination = BraboPreferencesTree.StorageCategory.Wallet.class, order = 1)
    String transactionHistoryFile();
}
