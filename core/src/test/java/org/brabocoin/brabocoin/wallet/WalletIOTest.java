package org.brabocoin.brabocoin.wallet;

import org.brabocoin.brabocoin.config.BraboConfig;
import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.crypto.cipher.BouncyCastleAES;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.dal.UTXODatabase;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.model.crypto.KeyPair;
import org.brabocoin.brabocoin.model.crypto.PrivateKey;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.testutil.LegacyBraboConfig;
import org.brabocoin.brabocoin.testutil.MockLegacyConfig;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.testutil.TestState;
import org.brabocoin.brabocoin.util.Destructible;
import org.brabocoin.brabocoin.wallet.generation.SecureRandomKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletIOTest {

    static MockLegacyConfig defaultConfig = new MockLegacyConfig(new LegacyBraboConfig(new BraboConfig()));
    private static final String walletPath = "src/test/resources/testwallet.dat";
    private static final String txHistPath = "src/test/resources/testtxhist.dat";
    private static final File walletFile = new File(walletPath);
    private static final File txhistFile = new File(txHistPath);
    private EllipticCurve curve = EllipticCurve.secp256k1();

    @BeforeEach
    void beforeEach() {
        defaultConfig = new MockLegacyConfig(defaultConfig) {
            @Override
            public String walletFile() {
                return walletPath;
            }

            @Override
            public String transactionHistoryFile() {
                return txHistPath;
            }
        };

        if (walletFile.exists()) {
            walletFile.delete();
        }

        if (txhistFile.exists()) {
            txhistFile.delete();
        }
    }

    @Test
    void readAndWriteSinglePlainKey() throws CipherException, IOException, DestructionException,
                                             DatabaseException {
        String passphrase = "MergeMuppet";

        PrivateKey key = PrivateKey.plain(BigInteger.valueOf(12345));

        State state = new TestState(defaultConfig);

        Wallet wallet = new Wallet(
            Collections.singletonList(
                new KeyPair(
                    curve.getPublicKeyFromPrivateKey(key.getKey().getReference().get()),
                    key
                )
            ),
            new TransactionHistory(Collections.emptyMap(), Collections.emptyMap()),
            state.getConsensus(),
            state.getSigner(),
            new SecureRandomKeyGenerator(),
            new BouncyCastleAES(),
            state.getWalletChainUtxoDatabase(),
            state.getWalletPoolUtxoDatabase(),
            state.getChainUTXODatabase(),
            state.getPoolUTXODatabase(),
            state.getBlockchain(),
            state.getTransactionPool()
        );

        WalletIO io = state.getWalletIO();

        io.write(
            wallet,
            walletFile,
            txhistFile,
            new Destructible<>(passphrase::toCharArray)
        );

        Wallet readWallet = io.read(walletFile, txhistFile,
            new Destructible<>(passphrase::toCharArray),
            state.getConsensus(),
            state.getSigner(),
            new SecureRandomKeyGenerator(),
            new BouncyCastleAES(),
            state.getWalletChainUtxoDatabase(),
            state.getWalletPoolUtxoDatabase(),
            state.getChainUTXODatabase(),
            state.getPoolUTXODatabase(),
            state.getBlockchain(),
            state.getTransactionPool()
        );

        assertEquals(key, readWallet.getPrivateKeys().iterator().next());
    }

    @Test
    void readAndWriteSingleEncryptedKey() throws CipherException, IOException,
                                                 DestructionException, DatabaseException {
        String passphrase = "MergeMuppet";

        Destructible<BigInteger> value = new Destructible<>(() -> BigInteger.valueOf(12345));
        PublicKey publicKey = curve.getPublicKeyFromPrivateKey(value.getReference().get());

        PrivateKey key = PrivateKey.encrypted(
            value,
            new Destructible<>("jitPassphrase!"::toCharArray),
            new BouncyCastleAES()
        );

        State state = new TestState(defaultConfig);

        Wallet wallet = new Wallet(
            Collections.singletonList(new KeyPair(publicKey, key)),
            new TransactionHistory(Collections.emptyMap(), Collections.emptyMap()),
            state.getConsensus(),
            state.getSigner(),
            new SecureRandomKeyGenerator(),
            new BouncyCastleAES(),
            state.getWalletChainUtxoDatabase(),
            state.getWalletPoolUtxoDatabase(),
            state.getChainUTXODatabase(),
            state.getPoolUTXODatabase(),
            state.getBlockchain(),
            state.getTransactionPool()
        );

        WalletIO io = state.getWalletIO();

        io.write(
            wallet,
            walletFile,
            txhistFile,
            new Destructible<>(passphrase::toCharArray)
        );

        Wallet readWallet = io.read(
            walletFile, txhistFile,
            new Destructible<>(passphrase::toCharArray),
            state.getConsensus(),
            state.getSigner(),
            new SecureRandomKeyGenerator(),
            new BouncyCastleAES(),
            state.getWalletChainUtxoDatabase(),
            state.getWalletPoolUtxoDatabase(),
            state.getChainUTXODatabase(),
            state.getPoolUTXODatabase(),
            state.getBlockchain(),
            state.getTransactionPool()
        );

        assertEquals(key, readWallet.getPrivateKeys().iterator().next());
    }

    @Test
    void readAndWriteSingleEncryptedKeyInvalid() throws CipherException, IOException,
                                                        DestructionException, DatabaseException {
        String passphrase = "MergeMuppet";

        Destructible<BigInteger> value = new Destructible<>(() -> BigInteger.valueOf(12345));
        PublicKey publicKey = curve.getPublicKeyFromPrivateKey(value.getReference().get());

        PrivateKey key = PrivateKey.encrypted(
            value,
            new Destructible<>("jitPassphrase!"::toCharArray),
            new BouncyCastleAES()
        );

        State state = new TestState(defaultConfig);

        Wallet wallet = new Wallet(
            Collections.singletonList(new KeyPair(publicKey, key)),
            new TransactionHistory(Collections.emptyMap(), Collections.emptyMap()),
            state.getConsensus(),
            state.getSigner(),
            new SecureRandomKeyGenerator(),
            new BouncyCastleAES(),
            new UTXODatabase(new HashMapDB()),
            new UTXODatabase(new HashMapDB()),
            new UTXODatabase(new HashMapDB()),
            new UTXODatabase(new HashMapDB()),
            state.getBlockchain(),
            state.getTransactionPool()
        );

        WalletIO io = state.getWalletIO();

        io.write(
            wallet,
            walletFile,
            txhistFile,
            new Destructible<>(passphrase::toCharArray)
        );

        assertThrows(CipherException.class, () -> io.read(
            walletFile, txhistFile,
            new Destructible<>("fout"::toCharArray),
            state.getConsensus(),
            state.getSigner(),
            new SecureRandomKeyGenerator(),
            new BouncyCastleAES(),
            new UTXODatabase(new HashMapDB()),
            new UTXODatabase(new HashMapDB()),
            new UTXODatabase(new HashMapDB()),
            new UTXODatabase(new HashMapDB()),
            state.getBlockchain(),
            state.getTransactionPool()
        ));
    }


    @Test
    void readAndWriteMultiplePlainKeys() throws CipherException, IOException,
                                                DestructionException, DatabaseException {
        String passphrase = "MergeMuppetMaarDanEenStukkieLanger";

        List<KeyPair> keys = Simulation.repeatedBuilder(Simulation::randomBigInteger, 100).stream()
            .map(b -> new KeyPair(curve.getPublicKeyFromPrivateKey(b), PrivateKey.plain(b)))
            .collect(Collectors.toList());


        State state = new TestState(defaultConfig);

        Wallet wallet = new Wallet(
            keys,
            new TransactionHistory(Collections.emptyMap(), Collections.emptyMap()),
            state.getConsensus(),
            state.getSigner(),
            new SecureRandomKeyGenerator(),
            new BouncyCastleAES(),
            state.getWalletChainUtxoDatabase(),
            state.getWalletPoolUtxoDatabase(),
            state.getChainUTXODatabase(),
            state.getPoolUTXODatabase(),
            state.getBlockchain(),
            state.getTransactionPool()
        );

        WalletIO io = state.getWalletIO();

        io.write(
            wallet,
            walletFile,
            txhistFile,
            new Destructible<>(passphrase::toCharArray)
        );

        Wallet readWallet = io.read(walletFile,
            txhistFile,
            new Destructible<>(passphrase::toCharArray),
            state.getConsensus(),
            state.getSigner(),
            new SecureRandomKeyGenerator(),
            new BouncyCastleAES(),
            state.getWalletChainUtxoDatabase(),
            state.getWalletPoolUtxoDatabase(),
            state.getChainUTXODatabase(),
            state.getPoolUTXODatabase(),
            state.getBlockchain(),
            state.getTransactionPool()
        );

        assertEquals(
            keys.stream().map(KeyPair::getPrivateKey).collect(Collectors.toSet()),
            new HashSet<>(readWallet.getPrivateKeys())
        );
    }

    @Test
    void readAndWriteMultipleEncryptedKeys() throws CipherException, IOException,
                                                    DestructionException, DatabaseException {
        String passphrase = "MergeMuppetMaarDanEenStukkieLanger";

        List<KeyPair> keys = Simulation.repeatedBuilder(Simulation::randomBigInteger, 10).stream()
            .map(b -> {
                try {
                    return new KeyPair(
                        curve.getPublicKeyFromPrivateKey(b),
                        PrivateKey.encrypted(
                            new Destructible<>(() -> new BigInteger(b.toByteArray())),
                            new Destructible<>("jitPassphrase!"::toCharArray),
                            new BouncyCastleAES()
                        )
                    );
                }
                catch (CipherException | DestructionException e) {
                    e.printStackTrace();
                }
                return null;
            })
            .collect(Collectors.toList());


        State state = new TestState(defaultConfig);

        Wallet wallet = new Wallet(
            keys,
            new TransactionHistory(Collections.emptyMap(), Collections.emptyMap()),
            state.getConsensus(),
            state.getSigner(),
            new SecureRandomKeyGenerator(),
            new BouncyCastleAES(),
            state.getWalletChainUtxoDatabase(),
            state.getWalletPoolUtxoDatabase(),
            state.getChainUTXODatabase(),
            state.getPoolUTXODatabase(),
            state.getBlockchain(),
            state.getTransactionPool()
        );

        WalletIO io = state.getWalletIO();

        io.write(
            wallet,
            walletFile,
            txhistFile,
            new Destructible<>(passphrase::toCharArray)
        );

        Wallet readWallet = io.read(walletFile,
            txhistFile,
            new Destructible<>(passphrase::toCharArray),
            state.getConsensus(),
            state.getSigner(),
            new SecureRandomKeyGenerator(),
            new BouncyCastleAES(),
            state.getWalletChainUtxoDatabase(),
            state.getWalletPoolUtxoDatabase(),
            state.getChainUTXODatabase(),
            state.getPoolUTXODatabase(),
            state.getBlockchain(),
            state.getTransactionPool()
        );

        assertEquals(
            keys.stream().map(KeyPair::getPrivateKey).collect(Collectors.toSet()),
            new HashSet<>(readWallet.getPrivateKeys())
        );
    }
}
