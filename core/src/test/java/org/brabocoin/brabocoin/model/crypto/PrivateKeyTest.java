package org.brabocoin.brabocoin.model.crypto;

import org.brabocoin.brabocoin.crypto.cipher.BouncyCastleAES;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.util.Destructible;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class PrivateKeyTest {

    @Test
    void encryptedGetLocked() throws CipherException, DestructionException {
        Destructible<BigInteger> superSecretKey = new Destructible<>(Simulation::randomBigInteger);
        Destructible<char[]> superSecretPassword = new Destructible<>(
                "IkWilNietInRAMBlijven!"::toCharArray
        );

        PrivateKey encrypted = PrivateKey.encrypted(
                superSecretKey, superSecretPassword, new BouncyCastleAES()
        );

        assertNull(superSecretKey.getReference().get());
        assertNull(superSecretPassword.getReference().get());

        assertThrows(IllegalStateException.class, encrypted::getKey);
    }

    @Test
    void encryptedGetUnlockedWrongPassphrase() throws CipherException, DestructionException {
        Destructible<BigInteger> superSecretKey = new Destructible<>(Simulation::randomBigInteger);
        Destructible<char[]> superSecretPassword = new Destructible<>(
                "IkWilNietInRAMBlijven!"::toCharArray
        );

        PrivateKey encrypted = PrivateKey.encrypted(
                superSecretKey, superSecretPassword, new BouncyCastleAES()
        );

        assertTrue(superSecretKey.isDestroyed());
        assertNull(superSecretKey.getReference().get());

        assertTrue(superSecretPassword.isDestroyed());
        assertNull(superSecretPassword.getReference().get());

        assertThrows(CipherException.class, () -> encrypted.unlock(new Destructible<>(
                "NietKloppendWachtwoord:("::toCharArray
        )));
    }

    @Test
    void encryptedGetUnlock() throws CipherException, DestructionException {
        BigInteger bladiebla = Simulation.randomBigInteger();

        Destructible<BigInteger> superSecretKey = new Destructible<>(
                // Create copy
                () -> new BigInteger(bladiebla.toByteArray())
        );
        Destructible<char[]> superSecretPassword = new Destructible<>(
                "IkWilNietInRAMBlijven!"::toCharArray
        );

        PrivateKey encrypted = PrivateKey.encrypted(
                superSecretKey, superSecretPassword, new BouncyCastleAES()
        );

        assertNull(superSecretKey.getReference().get());
        assertNull(superSecretPassword.getReference().get());

        Destructible<char[]> destructibleUnlockPassphrase = new Destructible<>(
                "IkWilNietInRAMBlijven!"::toCharArray
        );
        encrypted.unlock(destructibleUnlockPassphrase);

        Destructible<BigInteger> value = encrypted.getKey();

        assertTrue(destructibleUnlockPassphrase.isDestroyed());
        assertNull(destructibleUnlockPassphrase.getReference().get());

        assertFalse(value.isDestroyed());
        assertEquals(bladiebla, value.getReference().get());
    }

    @Test
    void plain() throws DestructionException {
        PrivateKey plain = PrivateKey.plain(BigInteger.valueOf(12345));

        assertEquals(BigInteger.valueOf(12345), plain.getKey().getReference().get());
    }
}