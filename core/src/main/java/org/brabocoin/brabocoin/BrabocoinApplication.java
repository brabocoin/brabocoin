package org.brabocoin.brabocoin;

import com.beust.jcommander.JCommander;
import com.google.common.collect.Sets;
import javafx.application.Platform;
import org.brabocoin.brabocoin.cli.BraboArgs;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.node.state.DeploymentState;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.node.state.Unlocker;
import org.brabocoin.brabocoin.processor.BlockProcessor;
import org.brabocoin.brabocoin.services.Node;
import org.brabocoin.brabocoin.wallet.Wallet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     *     The configuration file to use.
     * @param walletUnlocker
     *     The wallet unlocker.
     * @throws DatabaseException
     *     When one of the databases could not be initialized.
     */
    public BrabocoinApplication(@NotNull BraboConfig config,
                                @NotNull Unlocker<Wallet> walletUnlocker) throws DatabaseException {
        state = new DeploymentState(config, walletUnlocker);
        storages = Sets.newHashSet(
            state.getBlockStorage(),
            state.getUtxoStorage(),
            state.getWalletUTXOStorage()
        );
    }

    /**
     * Initialize a new Brabocoin node instance.
     * <p>
     * All existing data is loaded from disk and the data model is gathered and processed in
     * every data holder class.
     *
     * @param configPath
     *     The configuration file to use.
     * @param walletUnlocker
     *     The wallet unlocker.
     * @throws DatabaseException
     *     When one of the databases could not be initialized.
     */
    public BrabocoinApplication(@Nullable String configPath, @NotNull Unlocker<Wallet> walletUnlocker) throws DatabaseException, IOException {
        this(getConfig(configPath), walletUnlocker);
    }

    /**
     * Initialize a new Brabocoin node instance.
     * <p>
     * All existing data is loaded from disk and the data model is gathered and processed in
     * every data holder class.
     *
     * @param walletUnlocker
     *     The wallet unlocker.
     * @throws DatabaseException
     *     When one of the databases could not be initialized.
     */
    public BrabocoinApplication(@NotNull Unlocker<Wallet> walletUnlocker) throws DatabaseException, IOException {
        this(getConfig(null), walletUnlocker);
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

        if (arguments.getLogLevel() != null) {
            Logger rootLogger = Logger.getLogger("org.brabocoin.brabocoin");
            ConsoleHandler consoleHandler = new ConsoleHandler();
            rootLogger.addHandler(
                consoleHandler
            );
            switch (arguments.getLogLevel().toLowerCase()) {
                case "all":
                    rootLogger.setLevel(Level.ALL);
                    break;
                case "finest":
                    rootLogger.setLevel(Level.FINEST);
                    break;
                case "finer":
                    rootLogger.setLevel(Level.FINER);
                    break;
                case "fine":
                    rootLogger.setLevel(Level.FINE);
                    break;
                case "warning":
                    rootLogger.setLevel(Level.WARNING);
                    break;
                default:
                    LOGGER.severe("Could not parse config level");
                    Platform.exit();
            }
            consoleHandler.setLevel(rootLogger.getLevel());
        }

        BraboConfig config = getConfig(arguments.getConfig());

        BrabocoinApplication application = new BrabocoinApplication(
            config,
            (creation, creator) -> creator.apply(arguments.getPassword())
        );
        application.start();
    }

    private static @NotNull BraboConfig getConfig(@NotNull String path) throws IOException {
        File configFile = new File(path);

        if (!configFile.exists()) {
            // Unpack the default config to use
            InputStream config = BrabocoinApplication.class.getResourceAsStream("/application.yaml");
            Files.copy(config, configFile.getAbsoluteFile().toPath());
        }

        return BraboConfigProvider.getConfigFromFile(path).bind("brabo", BraboConfig.class);
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
