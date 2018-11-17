package org.brabocoin.brabocoin;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.dal.LevelDB;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.dal.UTXODatabase;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.processor.BlockProcessor;
import org.brabocoin.brabocoin.processor.PeerProcessor;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.processor.UTXOProcessor;
import org.brabocoin.brabocoin.services.Node;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
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
    private final @NotNull Node node;

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
        storages = new HashSet<>();
        node = initializeNode(config);
    }

    private @NotNull Node initializeNode(@NotNull BraboConfig config) throws DatabaseException {
        Random unsecureRandom = new Random();
        Consensus consensus = new Consensus();
        Signer signer = new Signer(EllipticCurve.secp256k1());

        KeyValueStore blockStorage = new LevelDB(new File(config.blockStoreDirectory(), config.databaseDirectory()));
        storages.add(blockStorage);

        Blockchain blockchain = new Blockchain(
            new BlockDatabase(blockStorage, config),
            consensus
        );

        KeyValueStore utxoStorage = new LevelDB(new File(config.utxoStoreDirectory(), config.databaseDirectory()));
        storages.add(utxoStorage);

        ChainUTXODatabase chainUTXODatabase = new ChainUTXODatabase(utxoStorage, consensus);

        UTXOProcessor utxoProcessor = new UTXOProcessor(chainUTXODatabase);

        TransactionPool transactionPool = new TransactionPool(config, unsecureRandom);
        UTXODatabase poolUTXODatabase = new UTXODatabase(new HashMapDB());

        TransactionValidator transactionValidator = new TransactionValidator(
            consensus,
            blockchain.getMainChain(),
            transactionPool,
            chainUTXODatabase,
            poolUTXODatabase,
            signer
        );

        TransactionProcessor transactionProcessor = new TransactionProcessor(
            transactionValidator,
            transactionPool,
            chainUTXODatabase,
            poolUTXODatabase
        );

        BlockValidator blockValidator = new BlockValidator(
            consensus,
            transactionValidator,
            transactionProcessor,
            blockchain,
            chainUTXODatabase,
            signer
        );

        BlockProcessor blockProcessor = new BlockProcessor(
            blockchain,
            utxoProcessor,
            transactionProcessor,
            consensus,
            blockValidator
        );

        PeerProcessor peerProcessor = new PeerProcessor(new HashSet<>(), config);

        return new Node(
            config.listenPort(),
            new NodeEnvironment(
                config.servicePort(),
                blockchain,
                blockProcessor,
                peerProcessor,
                transactionPool,
                transactionProcessor,
                config
            )
        );
    }

    public static void main(String[] args) throws DatabaseException, IOException {
        BrabocoinApplication application = new BrabocoinApplication(DEFAULT_CONFIG);
        application.start();
    }

    /**
     * Start the network node and the application in full.
     *
     * @throws IOException When the network node could not be started.
     * @see Node#start()
     */
    public void start() throws IOException {
        node.start();
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
            node.stopAndBlock();
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

    /**
     * Get the node environment for this application.
     *
     * @return The node environment.
     */
    public @NotNull NodeEnvironment getNodeEnvironment() {
        return node.getEnvironment();
    }
}
