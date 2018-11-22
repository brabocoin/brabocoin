package org.brabocoin.brabocoin.wallet;

import org.brabocoin.brabocoin.crypto.EllipticCurve;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.crypto.cipher.BouncyCastleAES;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.model.crypto.PrivateKey;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.util.Destructible;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletIOTest {
    private static final File walletFile = new File("src/test/resources/testwallet.dat");
    private EllipticCurve curve = EllipticCurve.secp256k1();

    @BeforeAll
    static void setUp() {
        if (walletFile.exists()) {
            walletFile.delete();
        }
    }

    @Test
    void readAndWriteSinglePlainKey() throws CipherException, IOException, DestructionException {
        String passphrase = "MergeMuppet";

        PrivateKey key = PrivateKey.plain(BigInteger.valueOf(12345));

        Wallet wallet = new Wallet(
                Collections.singletonMap(curve.getPublicKeyFromPrivateKey(key.getKey().getReference().get()), key)
        );

        WalletIO io = new WalletIO(
                new BouncyCastleAES()
        );

        io.write(wallet, walletFile, new Destructible<>(passphrase::toCharArray));

        Wallet readWallet = io.read(walletFile, new Destructible<>(passphrase::toCharArray));

        assertEquals(key, readWallet.getPrivateKeys().iterator().next());
    }

    @Test
    void readAndWriteSingleEncryptedKey() throws CipherException, IOException, DestructionException {
        String passphrase = "MergeMuppet";

        Destructible<BigInteger> value = new Destructible<>(() -> BigInteger.valueOf(12345));
        PublicKey publicKey = curve.getPublicKeyFromPrivateKey(value.getReference().get());

        PrivateKey key = PrivateKey.encrypted(
                value,
                new Destructible<>("jitPassphrase!"::toCharArray),
                new BouncyCastleAES()
        );

        Wallet wallet = new Wallet(
                Collections.singletonMap(publicKey, key)
        );

        WalletIO io = new WalletIO(
                new BouncyCastleAES()
        );

        io.write(wallet, walletFile, new Destructible<>(passphrase::toCharArray));

        Wallet readWallet = io.read(walletFile, new Destructible<>(passphrase::toCharArray));

        assertEquals(key, readWallet.getPrivateKeys().iterator().next());
    }


    @Test
    void readAndWriteMultiplePlainKeys() throws CipherException, IOException, DestructionException {
        String passphrase = "MergeMuppetMaarDanEenStukkieLanger";

        Map<PublicKey, PrivateKey> keys = Simulation.repeatedBuilder(Simulation::randomBigInteger, 100).stream()
                .collect(Collectors.toMap(
                        b -> curve.getPublicKeyFromPrivateKey(b),
                        PrivateKey::plain
                ));


        Wallet wallet = new Wallet(keys);

        WalletIO io = new WalletIO(
                new BouncyCastleAES()
        );

        io.write(wallet, walletFile, new Destructible<>(passphrase::toCharArray));

        Wallet readWallet = io.read(walletFile, new Destructible<>(passphrase::toCharArray));

        assertEquals(new HashSet<>(keys.values()), new HashSet<>(readWallet.getPrivateKeys()));
    }

    @Test
    void readAndWriteMultipleEncryptedKeys() throws CipherException, IOException, DestructionException {
        String passphrase = "MergeMuppetMaarDanEenStukkieLanger";

        Map<PublicKey, PrivateKey> keys = Simulation.repeatedBuilder(Simulation::randomBigInteger, 10).stream()
                .collect(Collectors.toMap(
                        b -> curve.getPublicKeyFromPrivateKey(b),
                        b -> {
                            try {
                                return PrivateKey.encrypted(
                                        new Destructible<>(() -> new BigInteger(b.toByteArray())),
                                        new Destructible<>("jitPassphrase!"::toCharArray),
                                        new BouncyCastleAES()
                                );
                            } catch (CipherException | DestructionException e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                ));


        Wallet wallet = new Wallet(keys);

        WalletIO io = new WalletIO(
                new BouncyCastleAES()
        );

        io.write(wallet, walletFile, new Destructible<>(passphrase::toCharArray));

        Wallet readWallet = io.read(walletFile, new Destructible<>(passphrase::toCharArray));

        assertEquals(new HashSet<>(keys.values()), new HashSet<>(readWallet.getPrivateKeys()));
    }
}