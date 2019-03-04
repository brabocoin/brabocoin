package org.brabocoin.brabocoin.gui.config;

import com.dlsc.preferencesfx.PreferencesFx;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import org.brabocoin.brabocoin.BrabocoinApplication;
import org.brabocoin.brabocoin.config.BraboConfig;
import org.brabocoin.brabocoin.validation.Consensus;

public class BraboPreferencesFx {

    public static PreferencesFx buildPreferencesFx(BraboConfig config, Consensus consensus) {
        return PreferencesFx.of(
            BrabocoinApplication.class,
            Category.of(
                "Network",
                Group.of(
                    "General",
                    Setting.of("Network ID", config.networkId),
                    Setting.of("Service port number", config.servicePort),
                    Setting.of("Target peer count", config.targetPeerCount),
                    Setting.of("Update peer interval (s)", config.updatePeerInterval),
                    Setting.of("Allow local peers", config.allowLocalPeers)
                ),
                Group.of(
                    "Advanced",
                    Setting.of(
                        "Number of orphan blocks before syncing",
                        config.maxSequentialOrphanBlocks
                    ).withDescription("Placeholder"),
                    Setting.of("Message processing interval (ms)", config.loopInterval),
                    Setting.of("Handshake response deadline (ms)", config.handshakeDeadline)
                )
            ),
            Category.of(
                "Storage",
                Group.of("General",
                    Setting.of("Data directory", config.dataDirectory),
                    Setting.of("Database subdirectory", config.databaseDirectory),
                    Setting.of("Block storage subdirectory", config.blockStoreDirectory),
                    Setting.of("UTXO storage subdirectory", config.utxoStoreDirectory),
                    Setting.of("Wallet storage subdirectory", config.walletStoreDirectory)
                    ),
                Group.of("Wallet details",
                    Setting.of("Wallet file", config.walletFile),
                    Setting.of("Transaction history file", config.transactionHistoryFile)
                    ),
                Group.of("Transaction details",
                    Setting.of("Maximum transactions in pool", config.maxTransactionPoolSize),
                    Setting.of("Maximum orphan transactions in memory", config.maxOrphanTransactions),
                    Setting.of("Maximum rejected transactions in memory", config.maxRecentRejectTransactions)
                    ),
                Group.of("Block details",
                    Setting.of("Maximum block storage file size (bytes)", config.maxBlockFileSize),
                    Setting.of("Maximum orphan blocks in memory", config.maxOrphanBlocks),
                    Setting.of("Maximum rejected blocks in memory", config.maxRecentRejectBlocks)
                    )
            ),
            Category.of(
                "Consensus",
                Setting.of("Target value", consensus.targetValue)
            )
        );
    }
}
