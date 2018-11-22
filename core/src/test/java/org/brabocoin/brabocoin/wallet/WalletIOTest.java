package org.brabocoin.brabocoin.wallet;

import com.google.common.collect.Sets;
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
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletIOTest {
    private static final File walletFile = new File("src/test/resources/testwallet.dat");

    @BeforeAll
    static void setUp() {
        if (walletFile.exists()) {
            walletFile.delete();
        }
    }

    @Test
    void readAndWriteSinglePlainKey() throws CipherException, IOException {
        String passphrase = "MergeMuppet";

        PrivateKey key = PrivateKey.plain(BigInteger.valueOf(12345));

        Wallet wallet = new Wallet(
                Collections.singletonList(key)
        );

        WalletIO io = new WalletIO(
                new BouncyCastleAES()
        );

        io.write(wallet, walletFile, passphrase.toCharArray());

        Wallet readWallet = io.read(walletFile, passphrase.toCharArray());

        assertEquals(key, readWallet.iterator().next());
    }

    @Test
    void readAndWriteSingleEncryptedKey() throws CipherException, IOException, DestructionException {
        String passphrase = "MergeMuppet";

        PrivateKey key = PrivateKey.encrypted(
                new Destructible<>(() -> BigInteger.valueOf(12345)),
                new Destructible<>("jitPassphrase!"::toCharArray),
                new BouncyCastleAES()
        );

        Wallet wallet = new Wallet(
                Collections.singletonList(key)
        );

        WalletIO io = new WalletIO(
                new BouncyCastleAES()
        );

        io.write(wallet, walletFile, passphrase.toCharArray());

        Wallet readWallet = io.read(walletFile, passphrase.toCharArray());

        assertEquals(key, readWallet.iterator().next());
    }


    @Test
    void readAndWriteMultiplePlainKeys() throws CipherException, IOException {
        String passphrase = "MergeMuppetMaarDanEenStukkieLanger";

        List<PrivateKey> keys = Simulation.repeatedBuilder(Simulation::randomBigInteger, 100).stream()
                .map(PrivateKey::plain)
                .collect(Collectors.toList());


        Wallet wallet = new Wallet(keys);

        WalletIO io = new WalletIO(
                new BouncyCastleAES()
        );

        io.write(wallet, walletFile, passphrase.toCharArray());

        Wallet readWallet = io.read(walletFile, passphrase.toCharArray());

        assertEquals(new HashSet<>(keys), Sets.newHashSet(readWallet.iterator()));
    }

    @Test
    void readAndWriteMultipleEncryptedKeys() throws CipherException, IOException {
        String passphrase = "MergeMuppetMaarDanEenStukkieLanger";

        List<PrivateKey> keys = Simulation.repeatedBuilder(Simulation::randomBigInteger, 10).stream()
                .map(b -> {
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
                })
                .collect(Collectors.toList());


        Wallet wallet = new Wallet(keys);

        WalletIO io = new WalletIO(
                new BouncyCastleAES()
        );

        io.write(wallet, walletFile, passphrase.toCharArray());

        Wallet readWallet = io.read(walletFile, passphrase.toCharArray());

        assertEquals(new HashSet<>(keys), Sets.newHashSet(readWallet.iterator()));
    }
}