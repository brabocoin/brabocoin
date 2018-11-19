package org.brabocoin.brabocoin;

import com.google.common.collect.Sets;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.node.state.DeploymentState;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.processor.BlockProcessor;
import org.brabocoin.brabocoin.services.Node;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main Brabocoin application that runs the full node.
 */
public class BrabocoinApplication {

    private static final Logger LOGGER = Logger.getLogger(BrabocoinApplication.class.getName());

    /**
     * Default configuration file.
     */
    private static final BraboConfig DEFAULT_CONFIG = BraboConfigProvider.getConfig().bind("brabo", BraboConfig.class);

    /**
     * The node.
     */
    private final @NotNull State state;

    /**
     * Set of storage objects that are created.
     *
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
     * @throws DatabaseException
     *     When one of the databases could not be initialized.
     */
    public BrabocoinApplication(@NotNull BraboConfig config) throws DatabaseException {
        state = new DeploymentState(config);
        storages = Sets.newHashSet(state.getBlockStorage(), state.getUtxoStorage());
    }

    public static void main(String[] args) throws DatabaseException, IOException {
        BrabocoinApplication application = new BrabocoinApplication(DEFAULT_CONFIG);
        application.start();
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
     * @see BlockProcessor#syncMainChainWithUTXOSet(ChainUTXODatabase)
     * @see Node#start()
     */
    public void start() throws IOException, DatabaseException, IllegalStateException {
        state.getEnvironment().syncMainChainWithUTXOSet();
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
}