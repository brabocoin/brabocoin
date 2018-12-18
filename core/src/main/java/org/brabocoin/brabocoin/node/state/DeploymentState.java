package org.brabocoin.brabocoin.node.state;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.crypto.cipher.BouncyCastleAES;
import org.brabocoin.brabocoin.crypto.cipher.Cipher;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.dal.CompositeReadonlyUTXOSet;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.dal.LevelDB;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.dal.UTXODatabase;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.exceptions.StateInitializationException;
import org.brabocoin.brabocoin.mining.Miner;
import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.processor.BlockProcessor;
import org.brabocoin.brabocoin.processor.PeerProcessor;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.processor.UTXOProcessor;
import org.brabocoin.brabocoin.services.Node;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.brabocoin.brabocoin.wallet.TransactionHistory;
import org.brabocoin.brabocoin.wallet.Wallet;
import org.brabocoin.brabocoin.wallet.WalletIO;
import org.brabocoin.brabocoin.wallet.generation.KeyGenerator;
import org.brabocoin.brabocoin.wallet.generation.SecureRandomKeyGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

/**
 * Data holder for all objects used by the node.
 */
public class DeploymentState implements State {

    protected final @NotNull BraboConfig config;
    protected final @NotNull Random unsecureRandom;
    protected final @NotNull Consensus consensus;
    protected final @NotNull Signer signer;
    protected final @NotNull Wallet wallet;
    protected final @NotNull WalletIO walletIO;
    protected final @NotNull KeyValueStore blockStorage;
    protected final @NotNull KeyValueStore utxoStorage;
    protected final @NotNull KeyValueStore walletUTXOStorage;
    protected final @NotNull BlockDatabase blockDatabase;
    protected final @NotNull ChainUTXODatabase chainUTXODatabase;
    protected final @NotNull UTXODatabase poolUTXODatabase;
    protected final @NotNull UTXODatabase walletUTXODatabase;
    protected final @NotNull Blockchain blockchain;
    protected final @NotNull TransactionPool transactionPool;
    protected final @NotNull BlockProcessor blockProcessor;
    protected final @NotNull UTXOProcessor utxoProcessor;
    protected final @NotNull TransactionProcessor transactionProcessor;
    protected final @NotNull PeerProcessor peerProcessor;
    protected final @NotNull TransactionValidator transactionValidator;
    protected final @NotNull BlockValidator blockValidator;
    protected final @NotNull Miner miner;
    protected final @NotNull NodeEnvironment environment;
    protected final @NotNull Node node;

    public DeploymentState(@NotNull BraboConfig config, @NotNull Unlocker<Wallet> walletUnlocker) throws DatabaseException {
        this.config = config;

        unsecureRandom = createUnsecureRandom();

        consensus = createConsensus();

        signer = createSigner();


        blockStorage = createBlockStorage();
        utxoStorage = createUtxoStorage();
        walletUTXOStorage = createWalletUTXOStorage();

        blockDatabase = createBlockDatabase();
        chainUTXODatabase = createChainUTXODatabase();
        poolUTXODatabase = createPoolUTXODatabase();
        walletUTXODatabase = createWalletUTXODatabase();


        blockchain = createBlockchain();

        transactionPool = createTransactionPool();

        transactionValidator = createTransactionValidator();
        transactionProcessor = createTransactionProcessor();

        utxoProcessor = createUtxoProcessor();

        blockValidator = createBlockValidator();
        blockProcessor = createBlockProcessor();

        peerProcessor = createPeerProcessor();

        miner = createMiner();

        try {
            walletIO = createWalletIO();
        }
        catch (CipherException e) {
            throw new StateInitializationException("Could not create wallet I/O object.", e);
        }

        try {
            wallet = createWallet(walletUnlocker);
        }
        catch (CipherException | DestructionException | IOException e) {
            throw new StateInitializationException("Could not create wallet.", e);
        }
        environment = createEnvironment();

        node = createNode();
    }

    private WalletIO createWalletIO() throws CipherException {
        return new WalletIO(
            new BouncyCastleAES()
        );
    }

    private Wallet createWallet(Unlocker<Wallet> walletUnlocker) throws CipherException, IOException, DestructionException {
        Cipher privateKeyCipher = new BouncyCastleAES();
        KeyGenerator keyGenerator = new SecureRandomKeyGenerator();

        Wallet created;
        File walletFile = Paths.get(
            config.dataDirectory(),
            config.walletStoreDirectory(),
            config.walletFile()
        ).toFile();
        File txHistoryFile = Paths.get(
            config.dataDirectory(),
            config.walletStoreDirectory(),
            config.transactionHistoryFile()
        ).toFile();
        ReadonlyUTXOSet watchUTXOSet = new CompositeReadonlyUTXOSet(chainUTXODatabase, poolUTXODatabase);
        if (walletFile.exists()) {
            // Read wallet
            created = walletUnlocker.unlock(false, passphrase -> {
                Wallet wallet;
                try {
                    wallet = walletIO.read(
                        walletFile,
                        txHistoryFile,
                        passphrase,
                        consensus,
                        signer,
                        keyGenerator,
                        privateKeyCipher,
                        walletUTXODatabase,
                        watchUTXOSet,
                        blockchain
                    );

                    passphrase.destruct();
                }
                catch (CipherException e) {
                    // Unlock failed because of bad password
                    return null;
                }
                catch (IOException | DestructionException e) {
                    throw new StateInitializationException("Could not create wallet.", e);
                }

                return wallet;
            });

        } else {
            // Create new wallet
            created = walletUnlocker.unlock(true, passphrase -> {
                Wallet wallet = new Wallet(
                    Collections.emptyList(),
                    new TransactionHistory(Collections.emptyMap(), Collections.emptyMap()),
                    consensus,
                    signer,
                    keyGenerator,
                    privateKeyCipher,
                    walletUTXODatabase,
                    watchUTXOSet,
                    blockchain
                );

                try {
                    wallet.generatePlainKeyPair();
                    walletIO.write(wallet, walletFile, txHistoryFile, passphrase);
                    passphrase.destruct();
                }
                catch (DestructionException | CipherException | IOException e) {
                    throw new StateInitializationException("Could not create wallet.", e);
                }

                return wallet;
            });
        }

        if (created == null) {
            throw new StateInitializationException("Could not create wallet.");
        }

        return created;
    }

    protected Random createUnsecureRandom() {
        return new Random();
    }

    protected Consensus createConsensus() {
        return new Consensus();
    }

    protected Signer createSigner() {
        return new Signer(consensus.getCurve());
    }

    protected KeyValueStore createBlockStorage() {
        return new LevelDB(Paths.get(
            config.dataDirectory(),
            Integer.toString(config.networkId()),
            config.blockStoreDirectory(),
            config.databaseDirectory()
        ).toFile());
    }

    protected KeyValueStore createUtxoStorage() {
        return new LevelDB(Paths.get(
            config.dataDirectory(),
            Integer.toString(config.networkId()),
            config.utxoStoreDirectory(),
            config.databaseDirectory()
        ).toFile());
    }

    protected KeyValueStore createWalletUTXOStorage() {
        return new LevelDB(Paths.get(
            config.dataDirectory(),
            config.walletStoreDirectory(),
            config.databaseDirectory()
        ).toFile());
    }

    protected BlockDatabase createBlockDatabase() throws DatabaseException {
        return new BlockDatabase(
            blockStorage,
            Paths.get(
                config.dataDirectory(),
                Integer.toString(config.networkId()),
                config.blockStoreDirectory()
            ).toFile(),
            config.maxBlockFileSize()
        );
    }

    protected ChainUTXODatabase createChainUTXODatabase() throws DatabaseException {
        return new ChainUTXODatabase(utxoStorage, consensus);
    }

    protected UTXODatabase createPoolUTXODatabase() throws DatabaseException {
        return new UTXODatabase(new HashMapDB());
    }

    protected UTXODatabase createWalletUTXODatabase() throws DatabaseException {
        return new UTXODatabase(walletUTXOStorage);
    }

    protected Blockchain createBlockchain() throws DatabaseException {
        return new Blockchain(blockDatabase, consensus, config.maxOrphanBlocks(), unsecureRandom);
    }

    protected TransactionPool createTransactionPool() {
        return new TransactionPool(
            config.maxTransactionPoolSize(),
            config.maxOrphanTransactions(),
            unsecureRandom
        );
    }

    protected BlockProcessor createBlockProcessor() {
        return new BlockProcessor(
            blockchain,
            utxoProcessor,
            transactionProcessor,
            consensus,
            blockValidator
        );
    }

    protected UTXOProcessor createUtxoProcessor() {
        return new UTXOProcessor(chainUTXODatabase);
    }

    protected TransactionProcessor createTransactionProcessor() {
        return new TransactionProcessor(transactionValidator, transactionPool, poolUTXODatabase);
    }

    protected PeerProcessor createPeerProcessor() {
        return new PeerProcessor(new HashSet<>(), config);
    }

    protected TransactionValidator createTransactionValidator() {
        return new TransactionValidator(this);
    }

    protected BlockValidator createBlockValidator() {
        return new BlockValidator(this);
    }

    protected Miner createMiner() {
        return new Miner(transactionPool, consensus, unsecureRandom, config.networkId());
    }

    protected NodeEnvironment createEnvironment() {
        return new NodeEnvironment(this);
    }

    protected Node createNode() {
        return new Node(environment, config.servicePort(), config.networkId());
    }

    @NotNull
    @Override
    public BraboConfig getConfig() {
        return config;
    }

    @NotNull
    @Override
    public Consensus getConsensus() {
        return consensus;
    }

    @NotNull
    @Override
    public Signer getSigner() {
        return signer;
    }

    @NotNull
    @Override
    public KeyValueStore getBlockStorage() {
        return blockStorage;
    }

    @NotNull
    @Override
    public KeyValueStore getUtxoStorage() {
        return utxoStorage;
    }

    @NotNull
    @Override
    public KeyValueStore getWalletUTXOStorage() {
        return walletUTXOStorage;
    }

    @NotNull
    @Override
    public BlockDatabase getBlockDatabase() {
        return blockDatabase;
    }

    @NotNull
    @Override
    public ChainUTXODatabase getChainUTXODatabase() {
        return chainUTXODatabase;
    }

    @NotNull
    @Override
    public UTXODatabase getPoolUTXODatabase() {
        return poolUTXODatabase;
    }

    @NotNull
    @Override
    public UTXODatabase getWalletUTXODatabase() {
        return walletUTXODatabase;
    }

    @NotNull
    @Override
    public Blockchain getBlockchain() {
        return blockchain;
    }

    @NotNull
    @Override
    public TransactionPool getTransactionPool() {
        return transactionPool;
    }

    @NotNull
    @Override
    public BlockProcessor getBlockProcessor() {
        return blockProcessor;
    }

    @NotNull
    @Override
    public UTXOProcessor getUtxoProcessor() {
        return utxoProcessor;
    }

    @NotNull
    @Override
    public TransactionProcessor getTransactionProcessor() {
        return transactionProcessor;
    }

    @NotNull
    @Override
    public PeerProcessor getPeerProcessor() {
        return peerProcessor;
    }

    @NotNull
    @Override
    public TransactionValidator getTransactionValidator() {
        return transactionValidator;
    }

    @NotNull
    @Override
    public BlockValidator getBlockValidator() {
        return blockValidator;
    }

    @NotNull
    @Override
    public Miner getMiner() {
        return miner;
    }

    @NotNull
    @Override
    public NodeEnvironment getEnvironment() {
        return environment;
    }

    @NotNull
    @Override
    public Node getNode() {
        return node;
    }

    @Override
    public @NotNull Wallet getWallet() {
        return wallet;
    }

    @Override
    public @NotNull WalletIO getWalletIO() {
        return walletIO;
    }
}
