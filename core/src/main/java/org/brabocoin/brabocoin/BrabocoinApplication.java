package org.brabocoin.brabocoin;

import com.beust.jcommander.JCommander;
import com.google.common.collect.Sets;
import javafx.util.Pair;
import org.brabocoin.brabocoin.cli.BraboArgs;
import org.brabocoin.brabocoin.config.BraboConfig;
import org.brabocoin.brabocoin.config.BraboConfigAdapter;
import org.brabocoin.brabocoin.config.MutableBraboConfig;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.node.state.DeploymentState;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.node.state.Unlocker;
import org.brabocoin.brabocoin.processor.BlockProcessor;
import org.brabocoin.brabocoin.services.Node;
import org.brabocoin.brabocoin.util.ConfigUtil;
import org.brabocoin.brabocoin.util.LoggingUtil;
import org.brabocoin.brabocoin.validation.consensus.Consensus;
import org.brabocoin.brabocoin.validation.consensus.MutableConsensus;
import org.brabocoin.brabocoin.wallet.Wallet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Main Brabocoin application that runs the full node.
 */
public class BrabocoinApplication {

    private static final Logger LOGGER = Logger.getLogger(BrabocoinApplication.class.getName());

    /**
     * The node.
     */
    private final @NotNull State state;

    /**
     * Set of storage objects that are created.
     * <p>
     * These are maintained such that they can be properly closed when the application shuts down.
     */
    private final @NotNull Set<KeyValueStore> storages;

    /**
     * Initialize a new Brabocoin node instance.
     * <p>
     * All existing data is loaded from disk and the data model is gathered and processed in
     * every data holder class.
     *
     * @param config
     *     The config to use
     * @param consensus
     *     The consensus to use
     * @param walletUnlocker
     *     The wallet unlocker.
     * @throws DatabaseException
     *     When one of the databases could not be initialized.
     */
    public BrabocoinApplication(MutableBraboConfig config,
                                MutableConsensus consensus,
                                @NotNull Unlocker<Wallet> walletUnlocker) throws DatabaseException {
        BraboConfig immutableConfig = new BraboConfigAdapter(config);
        Consensus immutableConsensus = new Consensus(consensus);

        state = new DeploymentState(
            immutableConfig,
            immutableConsensus,
            walletUnlocker
        );

        storages = Sets.newHashSet(
            state.getBlockStorage(),
            state.getUtxoStorage(),
            state.getWalletChainUtxoStorage(),
            state.getWalletPoolUtxoStorage()
        );
    }

    public static void main(String[] args) throws DatabaseException, IOException {
        BraboArgs arguments = new BraboArgs();

        JCommander commander = JCommander.newBuilder()
            .addObject(arguments)
            .build();

        commander.parse(args);

        if (arguments.isHelp()) {
            commander.usage();
            return;
        }

        LoggingUtil.setLogLevel(arguments.getLogLevel());

        Pair<MutableBraboConfig, MutableConsensus> configPair = getConfigPair(
            arguments.getConfig(),
            true
        );

        BrabocoinApplication application = new BrabocoinApplication(
            configPair.getKey(),
            configPair.getValue(),
            (creation, creator) -> creator.apply(arguments.getPassword())
        );
        application.start();
    }

    private static void dropConfig(File configFile) {
        try {
            ConfigUtil.write(new MutableBraboConfig(), new Consensus(), configFile);
        }
        catch (IOException | IllegalAccessException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static Pair<MutableBraboConfig, MutableConsensus> getConfigPair(
        String path, Boolean forceDrop) throws IOException {
        File configFile = path == null ? null : new File(path);

        if (!forceDrop) {
            if (configFile != null) {
                // Make sure PreferencesFX does not override parameter-passed config
                try {
                    Preferences.userNodeForPackage(BrabocoinApplication.class).clear();
                }
                catch (BackingStoreException e) {
                    // ignored
                }

                if (!configFile.exists()) {
                    dropConfig(configFile);
                }
            }
            else {
                return new Pair<>(new MutableBraboConfig(), new MutableConsensus());
            }
        }
        else {
            if (configFile == null) {
                configFile = new File(BraboArgs.defaultConfig);
            }

            if (!configFile.exists()) {
                dropConfig(configFile);
            }
        }

        try {
            return ConfigUtil.read(configFile);
        }
        catch (IllegalAccessException e) {
            return new Pair<>(new MutableBraboConfig(), new MutableConsensus());
        }
    }

    /**
     * Starts the application.
     * <p>
     * The following steps are executed, in order, to start the application:
     * <ol>
     * <li>Load the main chain in memory, setting the top block to the last processed block
     * stored in the chain UTXO set.</li>
     * <li>Start the network node.</li>
     * </ol>
     *
     * @throws IOException
     *     When the network node could not be started.
     * @throws DatabaseException
     *     When a database backend is not available.
     * @throws IllegalStateException
     *     When the stored data is not consistent and likely corrupted.
     * @see BlockProcessor#syncMainChainWithUTXOSet()
     * @see Node#start()
     */
    public void start() throws IOException, DatabaseException, IllegalStateException {
        state.getBlockProcessor().syncMainChainWithUTXOSet();
        state.getNode().start();

        addShutdownHook();
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void shutdown() {
        LOGGER.info("Shutting down application.");

        // Stop the node
        LOGGER.info("Stopping the network node.");
        try {
            state.getNode().stopAndBlock();
        }
        catch (InterruptedException e) {
            // TODO: What to do here?
        }

        // Close databases
        LOGGER.info("Closing all databases.");
        for (KeyValueStore db : storages) {
            if (db != null) {
                try {
                    db.close();
                }
                catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Database could not be closed", e);
                }
            }
        }
    }

    public @NotNull State getState() {
        return state;
    }
}
