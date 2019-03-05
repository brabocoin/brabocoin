package org.brabocoin.brabocoin.gui.config;

import com.dlsc.formsfx.model.validators.CustomValidator;
import com.dlsc.formsfx.model.validators.IntegerRangeValidator;
import com.dlsc.preferencesfx.PreferencesFx;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import org.brabocoin.brabocoin.BrabocoinApplication;
import org.brabocoin.brabocoin.config.MutableBraboConfig;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.validation.consensus.MutableConsensus;

public class BraboPreferencesFx {

    private static MutableBraboConfig config;
    private static MutableConsensus consensus;

    public static PreferencesFx buildPreferencesFx(MutableBraboConfig config,
                                                   MutableConsensus consensus) {
        BraboPreferencesFx.config = config;
        BraboPreferencesFx.consensus = consensus;
        return PreferencesFx.of(
            BrabocoinApplication.class,
            Category.of(
                "Network",
                Group.of(
                    "General",
                    Setting.of("Network ID", config.networkId)
                        .validate(IntegerRangeValidator.atLeast(
                            0,
                            "Network id should be positive."
                        )),
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
                    ),
                    Setting.of("Message processing interval (ms)", config.loopInterval),
                    Setting.of("Handshake response deadline (ms)", config.handshakeDeadline)
                )
            ),
            Category.of(
                "Storage",
                Group.of(
                    "General",
                    Setting.of("Data directory", config.dataDirectory),
                    Setting.of("Database subdirectory", config.databaseDirectory),
                    Setting.of("Block storage subdirectory", config.blockStoreDirectory),
                    Setting.of("UTXO storage subdirectory", config.utxoStoreDirectory),
                    Setting.of("Wallet storage subdirectory", config.walletStoreDirectory)
                ),
                Group.of(
                    "Wallet details",
                    Setting.of("Wallet file", config.walletFile),
                    Setting.of("Transaction history file", config.transactionHistoryFile)
                ),
                Group.of(
                    "Transaction details",
                    Setting.of("Maximum transactions in pool", config.maxTransactionPoolSize),
                    Setting.of(
                        "Maximum orphan transactions in memory",
                        config.maxOrphanTransactions
                    ),
                    Setting.of(
                        "Maximum rejected transactions in memory",
                        config.maxRecentRejectTransactions
                    )
                ),
                Group.of(
                    "Block details",
                    Setting.of("Maximum block storage file size (bytes)", config.maxBlockFileSize),
                    Setting.of("Maximum orphan blocks in memory", config.maxOrphanBlocks),
                    Setting.of("Maximum rejected blocks in memory", config.maxRecentRejectBlocks)
                )
            ),
            Category.of(
                "Consensus",
                Group.of(
                    "Blocks",
                    Setting.of("Target value", consensus.targetValue)
                        .withDescription(
                            "If the value start with a '0' character, the value is considered to "
                                + "be in hexadecimal format, otherwise it is parsed as a decimal.")
                        .validate(CustomValidator.forPredicate(
                            s -> ByteUtil.parseHash((String)s) != null,
                            "Invalid format"
                        )),
                    Setting.of("Block reward (brabocents)", consensus.blockReward),
                    Setting.of("Maximum nonce size (bytes)", consensus.maxNonceSize),
                    Setting.of("Maximum block size (bytes)", consensus.maxBlockSize)
                ),
                Group.of(
                    "Validation",
                    Setting.of(
                        "Minimum transaction fee (brabocents)",
                        consensus.minimumTransactionFee
                    ),
                    Setting.of("Coinbase maturity depth", consensus.coinbaseMaturityDepth)
                )
            )
                .withDescription(
                    "Beware, changes made to consensus may result in your data being out of sync "
                        + "with the network.")
                .withDescriptionStyle("-fx-font-size: 1.2em; -fx-text-fill: #256B9C")
        );
    }

    public static MutableBraboConfig getConfig() {
        return config;
    }

    public static MutableConsensus getConsensus() {
        return consensus;
    }
}
