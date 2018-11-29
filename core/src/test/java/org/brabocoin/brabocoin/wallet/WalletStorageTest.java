package org.brabocoin.brabocoin.wallet;

import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.crypto.cipher.BouncyCastleAES;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.model.crypto.KeyPair;
import org.brabocoin.brabocoin.model.crypto.PrivateKey;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.testutil.TestState;
import org.brabocoin.brabocoin.util.Destructible;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletStorageTest {

    static BraboConfig defaultConfig = BraboConfigProvider.getConfig()
        .bind("brabo", BraboConfig.class);
    private static final File walletFile = new File("src/test/resources/testwallet.dat");
    private EllipticCurve curve = EllipticCurve.secp256k1();

    @BeforeAll
    static void setUp() {
        if (walletFile.exists()) {
            walletFile.delete();
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
            state.getConsensus(),
            state.getSigner(),
            state.getKeyGenerator()
        );

        WalletStorage io = new WalletStorage(
            new BouncyCastleAES(), wallet
        );

        io.write(walletFile, new Destructible<>(passphrase::toCharArray));

        io.read(walletFile, new Destructible<>(passphrase::toCharArray),
            state.getConsensus(),
            state.getSigner(),
            state.getKeyGenerator()
        );

        assertEquals(key, io.getWallet().getPrivateKeys().iterator().next());
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
            state.getConsensus(),
            state.getSigner(),
            state.getKeyGenerator()
        );

        WalletStorage io = new WalletStorage(
            new BouncyCastleAES(), wallet
        );

        io.write(walletFile, new Destructible<>(passphrase::toCharArray));

        io.read(walletFile, new Destructible<>(passphrase::toCharArray), state.getConsensus(),
            state.getSigner(),
            state.getKeyGenerator()
        );

        assertEquals(key, io.getWallet().getPrivateKeys().iterator().next());
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
            state.getConsensus(),
            state.getSigner(),
            state.getKeyGenerator()
        );

        WalletStorage io = new WalletStorage(
            new BouncyCastleAES(), wallet
        );

        io.write(walletFile, new Destructible<>(passphrase::toCharArray));

        io.read(walletFile, new Destructible<>(passphrase::toCharArray), state.getConsensus(),
            state.getSigner(),
            state.getKeyGenerator()
        );

        assertEquals(
            keys.stream().map(KeyPair::getPrivateKey).collect(Collectors.toSet()),
            new HashSet<>(io.getWallet().getPrivateKeys())
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

        Wallet wallet = new Wallet(keys, state.getConsensus(),
            state.getSigner(),
            state.getKeyGenerator());

        WalletStorage io = new WalletStorage(
            new BouncyCastleAES(), wallet
        );

        io.write(walletFile, new Destructible<>(passphrase::toCharArray));

        io.read(walletFile, new Destructible<>(passphrase::toCharArray), state.getConsensus(),
            state.getSigner(),
            state.getKeyGenerator());

        assertEquals(
            keys.stream().map(KeyPair::getPrivateKey).collect(Collectors.toSet()),
            new HashSet<>(io.getWallet().getPrivateKeys())
        );
    }
}