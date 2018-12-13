package org.brabocoin.brabocoin.crypto.cipher;

import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.model.crypto.SaltedSecretKey;
import org.brabocoin.brabocoin.model.crypto.SecretKey;

public interface SecretKeyGenerator {
    /**
     * Create a secret key, creating a random salt when necessary
     * @param word
     * @return
     * @throws CipherException
     */
    SecretKey createSecretKey(char[] word) throws CipherException;

    /**
     * Create a secret key, given a salt.
     * @param word
     * @param salt
     * @return
     * @throws CipherException
     */
    SaltedSecretKey createSecretKey(char[] word, byte[] salt) throws CipherException;
}
