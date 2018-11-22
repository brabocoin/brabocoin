package org.brabocoin.brabocoin.crypto.cipher;

import org.brabocoin.brabocoin.exceptions.CipherException;

/**
 * Interface for a cipher.
 */
public interface Cipher {
    /**
     * Encrypt the input data, given a key.
     *
     * @param input The data to encrypt
     * @param word  The passphrase to encrypt the data with
     * @return Cipher data
     */
    byte[] encrypt(byte[] input, char[] word) throws CipherException;

    /**
     * Decrypt the input data, given a key.
     *
     * @param input The data to decrypt
     * @param word  The passphrase to decrypt the data with
     * @return Decrypted data
     */
    byte[] decyrpt(byte[] input, char[] word) throws CipherException;
}
