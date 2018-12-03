package org.brabocoin.brabocoin.node.state;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.crypto.cipher.Cipher;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.dal.UTXODatabase;
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
import org.jetbrains.annotations.NotNull;

/**
 * @author Sten Wessel
 */
public interface State {

    @NotNull BraboConfig getConfig();

    @NotNull Consensus getConsensus();

    @NotNull Signer getSigner();

    @NotNull KeyValueStore getBlockStorage();

    @NotNull KeyValueStore getUtxoStorage();

    @NotNull KeyValueStore getWalletUTXOStorage();

    @NotNull BlockDatabase getBlockDatabase();

    @NotNull ChainUTXODatabase getChainUTXODatabase();

    @NotNull UTXODatabase getPoolUTXODatabase();

    @NotNull UTXODatabase getWalletUTXODatabase();

    @NotNull Blockchain getBlockchain();

    @NotNull TransactionPool getTransactionPool();

    @NotNull BlockProcessor getBlockProcessor();

    @NotNull UTXOProcessor getUtxoProcessor();

    @NotNull TransactionProcessor getTransactionProcessor();

    @NotNull PeerProcessor getPeerProcessor();

    @NotNull TransactionValidator getTransactionValidator();

    @NotNull BlockValidator getBlockValidator();

    @NotNull Miner getMiner();

    @NotNull NodeEnvironment getEnvironment();

    @NotNull Node getNode();

    @NotNull WalletStorage getWalletStorage();

    @NotNull KeyGenerator getKeyGenerator();

    @NotNull Cipher getPrivateKeyCipher();
}
