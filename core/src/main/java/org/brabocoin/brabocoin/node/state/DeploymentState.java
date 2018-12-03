package org.brabocoin.brabocoin.node.state;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.crypto.cipher.BouncyCastleAES;
import org.brabocoin.brabocoin.crypto.cipher.Cipher;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.dal.LevelDB;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.dal.UTXODatabase;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
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
import org.brabocoin.brabocoin.wallet.WalletStorage;
import org.brabocoin.brabocoin.wallet.generation.KeyGenerator;
import org.brabocoin.brabocoin.wallet.generation.SecureRandomKeyGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
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
    protected final @NotNull KeyGenerator keyGenerator;
    protected final @NotNull WalletStorage walletStorage;
    protected final @NotNull Cipher privateKeyCipher;
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

    public DeploymentState(@NotNull BraboConfig config) throws DatabaseException {
        this.config = config;

        unsecureRandom = createUnsecureRandom();

        consensus = createConsensus();

        signer = createSigner();

        keyGenerator = createKeyGenerator();
        try {
            walletStorage = createWalletStorage();
            privateKeyCipher = createPrivateKeyCipher();
        }
        catch (CipherException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not create state.");
        }

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

        environment = createEnvironment();

        node = createNode();
    }

    private Cipher createPrivateKeyCipher() throws CipherException {
        return new BouncyCastleAES();
    }

    private WalletStorage createWalletStorage() throws CipherException {
        return new WalletStorage(new BouncyCastleAES());
    }

    protected KeyGenerator createKeyGenerator() {
        return new SecureRandomKeyGenerator();
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
        return new LevelDB(new File(config.blockStoreDirectory(), config.databaseDirectory()));
    }

    protected KeyValueStore createUtxoStorage() {
        return new LevelDB(new File(config.utxoStoreDirectory(), config.databaseDirectory()));
    }

    protected KeyValueStore createWalletUTXOStorage() {
        return new LevelDB(new File(config.walletStoreDirectory(), config.databaseDirectory()));
    }

    protected BlockDatabase createBlockDatabase() throws DatabaseException {
        return new BlockDatabase(
            blockStorage,
            new File(config.blockStoreDirectory()),
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
        return new Node(environment, config.servicePort());
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

    @NotNull
    @Override
    public KeyGenerator getKeyGenerator() {
        return keyGenerator;
    }

    @NotNull
    @Override
    public WalletStorage getWalletStorage() {
        return walletStorage;
    }

    @NotNull
    @Override
    public Cipher getPrivateKeyCipher() {
        return privateKeyCipher;
    }

}
