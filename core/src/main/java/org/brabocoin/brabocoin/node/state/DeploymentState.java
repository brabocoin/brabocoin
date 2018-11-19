package org.brabocoin.brabocoin.node.state;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.dal.LevelDB;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.dal.UTXODatabase;
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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.Random;

/**
 * Data holder for all objects used by the node.
 */
public class DeploymentState implements State {

    private final @NotNull BraboConfig config;
    private final @NotNull Consensus consensus;

    private final @NotNull Signer signer;

    private final @NotNull KeyValueStore blockStorage;
    private final @NotNull KeyValueStore utxoStorage;

    private final @NotNull BlockDatabase blockDatabase;
    private final @NotNull ChainUTXODatabase chainUTXODatabase;
    private final @NotNull UTXODatabase poolUTXODatabase;

    private final @NotNull Blockchain blockchain;

    private final @NotNull TransactionPool transactionPool;

    private final @NotNull BlockProcessor blockProcessor;
    private final @NotNull UTXOProcessor utxoProcessor;
    private final @NotNull TransactionProcessor transactionProcessor;
    private final @NotNull PeerProcessor peerProcessor;

    private final @NotNull TransactionValidator transactionValidator;
    private final @NotNull BlockValidator blockValidator;

    private final @NotNull Miner miner;

    private final @NotNull NodeEnvironment environment;
    private final @NotNull Node node;

    public DeploymentState(@NotNull BraboConfig config) throws DatabaseException {
        Random unsecureRandom = new Random();

        this.config = config;
        consensus = new Consensus();

        signer = new Signer(consensus.getCurve());

        blockStorage = new LevelDB(new File(config.blockStoreDirectory(), config.databaseDirectory()));
        utxoStorage = new LevelDB(new File(config.utxoStoreDirectory(), config.databaseDirectory()));

        blockDatabase = new BlockDatabase(blockStorage, new File(config.blockStoreDirectory()), config.maxBlockFileSize());
        chainUTXODatabase = new ChainUTXODatabase(utxoStorage, consensus);
        poolUTXODatabase = new UTXODatabase(new HashMapDB());

        blockchain = new Blockchain(blockDatabase, consensus);

        transactionPool = new TransactionPool(config.maxTransactionPoolSize(), config.maxOrphanTransactions(), unsecureRandom);

        transactionValidator = new TransactionValidator(this);
        transactionProcessor = new TransactionProcessor(transactionValidator, transactionPool, chainUTXODatabase, poolUTXODatabase);

        utxoProcessor = new UTXOProcessor(chainUTXODatabase);


        blockValidator = new BlockValidator(this);
        blockProcessor = new BlockProcessor(blockchain, utxoProcessor, transactionProcessor, consensus, blockValidator);

        peerProcessor = new PeerProcessor(new HashSet<>(), config);

        miner = new Miner(transactionPool, consensus, unsecureRandom);

        environment = new NodeEnvironment(this);

        node = new Node(environment, config.servicePort());
    }

    @Override
    public @NotNull BraboConfig getConfig() {
        return config;
    }

    @Override
    public @NotNull Consensus getConsensus() {
        return consensus;
    }

    @Override
    public @NotNull Signer getSigner() {
        return signer;
    }

    @Override
    public @NotNull KeyValueStore getBlockStorage() {
        return blockStorage;
    }

    @Override
    public @NotNull KeyValueStore getUtxoStorage() {
        return utxoStorage;
    }

    @Override
    public @NotNull BlockDatabase getBlockDatabase() {
        return blockDatabase;
    }

    @Override
    public @NotNull ChainUTXODatabase getChainUTXODatabase() {
        return chainUTXODatabase;
    }

    @Override
    public @NotNull UTXODatabase getPoolUTXODatabase() {
        return poolUTXODatabase;
    }

    @Override
    public @NotNull Blockchain getBlockchain() {
        return blockchain;
    }

    @Override
    public @NotNull TransactionPool getTransactionPool() {
        return transactionPool;
    }

    @Override
    public @NotNull BlockProcessor getBlockProcessor() {
        return blockProcessor;
    }

    @Override
    public @NotNull UTXOProcessor getUtxoProcessor() {
        return utxoProcessor;
    }

    @Override
    public @NotNull TransactionProcessor getTransactionProcessor() {
        return transactionProcessor;
    }

    @Override
    public @NotNull PeerProcessor getPeerProcessor() {
        return peerProcessor;
    }

    @Override
    public @NotNull TransactionValidator getTransactionValidator() {
        return transactionValidator;
    }

    @Override
    public @NotNull BlockValidator getBlockValidator() {
        return blockValidator;
    }

    @Override
    public @NotNull Miner getMiner() {
        return miner;
    }

    @Override
    public @NotNull NodeEnvironment getEnvironment() {
        return environment;
    }

    @Override
    public @NotNull Node getNode() {
        return node;
    }
}
