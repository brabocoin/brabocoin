package org.brabocoin.brabocoin.wallet;

import org.brabocoin.brabocoin.config.BraboConfig;
import org.brabocoin.brabocoin.config.BraboConfigProvider;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.UnsignedTransaction;
import org.brabocoin.brabocoin.model.crypto.KeyPair;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.testutil.MockBraboConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.testutil.TestState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class WalletTest {

    static BraboConfig defaultConfig = BraboConfigProvider.getConfig()
        .bind("brabo", BraboConfig.class);
    private static final String walletPath = "testwallet.dat";
    private static final File walletFile = Paths.get(
        defaultConfig.dataDirectory(),
        Integer.toString(defaultConfig.networkId()),
        defaultConfig.walletStoreDirectory(),
        walletPath
    ).toFile();

    @BeforeEach
    void beforeEach() {
        defaultConfig = new MockBraboConfig(defaultConfig) {
            @Override
            public String walletFile() {
                return walletPath;
            }
        };

        if (walletFile.exists()) {
            walletFile.delete();
        }
    }

    @Test
    void signTransactionValidPlain() throws DatabaseException, DestructionException {
        State state = new TestState(defaultConfig);

        Wallet wallet = state.getWallet();

        KeyPair plain = wallet.generatePlainKeyPair();

        Transaction coinbaseTx = Transaction.coinbase(new Output(
            plain.getPublicKey().getHash(), state.getConsensus().getBlockReward()
        ), 1);

        state.getWalletChainUtxoDatabase().setOutputsUnspent(coinbaseTx, 1);

        UnsignedTransaction utx = new UnsignedTransaction(
            Collections.singletonList(
                new Input(coinbaseTx.getHash(), 0)
            ),
            Collections.emptyList()
        );

        TransactionSigningResult result = wallet.signTransaction(utx);

        assertEquals(TransactionSigningStatus.SIGNED, result.getStatus());
        assertNotNull(result.getTransaction());
    }

    @Test
    void onTopBlockConnectedKnownOutput() throws DatabaseException, DestructionException {
        State state = new TestState(defaultConfig);
        Wallet wallet = state.getWallet();
        KeyPair plain = wallet.generatePlainKeyPair();

        Transaction coinbaseTx = Transaction.coinbase(new Output(
            plain.getPublicKey().getHash(), state.getConsensus().getBlockReward()
        ), 1);

        Block block = new Block(
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(coinbaseTx),
            0
        );

        state.getBlockchain().storeBlock(block, false);
        state.getBlockchain().pushTopBlock(state.getBlockchain().getIndexedBlock(block.getHash()));

        assertNotNull(wallet.getTransactionHistory()
            .findConfirmedTransaction(coinbaseTx.getHash()));
    }

    @Test
    void onTopBlockConnectedPromoteToConfirmed() throws DatabaseException {
        State state = new TestState(defaultConfig);
        Wallet wallet = state.getWallet();

        Transaction coinbaseTx = Transaction.coinbase(new Output(
            Simulation.randomHash(), state.getConsensus().getBlockReward()
        ), 1);

        wallet.getTransactionHistory().addUnconfirmedTransaction(
            new UnconfirmedTransaction(
                coinbaseTx,
                Instant.now().getEpochSecond(),
                state.getConsensus().getBlockReward()
            )
        );

        Block block = new Block(
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(coinbaseTx),
            0
        );

        state.getBlockchain().storeBlock(block, false);
        state.getBlockchain().pushTopBlock(state.getBlockchain().getIndexedBlock(block.getHash()));

        assertNotNull(wallet.getTransactionHistory()
            .findConfirmedTransaction(coinbaseTx.getHash()));
        assertNull(wallet.getTransactionHistory().findUnconfirmedTransaction(coinbaseTx.getHash()));
    }

    @Test
    void onTopBlockDisconnected() throws DatabaseException, DestructionException {
        State state = new TestState(defaultConfig);
        Wallet wallet = state.getWallet();
        KeyPair plain = wallet.generatePlainKeyPair();

        Transaction coinbaseTx = Transaction.coinbase(new Output(
            plain.getPublicKey().getHash(), state.getConsensus().getBlockReward()
        ), 1);

        Block block = new Block(
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomBigInteger(),
            1,
            Collections.singletonList(coinbaseTx),
            0
        );

        state.getBlockchain().storeBlock(block, false);
        state.getBlockchain().pushTopBlock(state.getBlockchain().getIndexedBlock(block.getHash()));
        state.getBlockchain().popTopBlock();

        assertNull(wallet.getTransactionHistory().findConfirmedTransaction(coinbaseTx.getHash()));
        assertNotNull(wallet.getTransactionHistory()
            .findUnconfirmedTransaction(coinbaseTx.getHash()));
    }
}
