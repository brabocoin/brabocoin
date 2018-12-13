package org.brabocoin.brabocoin.crypto.cipher;

import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.model.crypto.SaltedSecretKey;

import javax.crypto.spec.SecretKeySpec;

/**
 * Key cipher, i.e. symmetric encryption
 */
public abstract class KeyCipher implements Cipher, SecretKeyGenerator {
    /**
     * Create key specification for this cipher.
     *
     * @param saltedSecretKey The key to create a spec for.
     * @return {@link SecretKeySpec} object.
     */
    abstract SecretKeySpec createKeySpec(SaltedSecretKey saltedSecretKey) throws CipherException;
}
