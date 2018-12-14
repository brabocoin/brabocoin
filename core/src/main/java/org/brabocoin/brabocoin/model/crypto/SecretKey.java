package org.brabocoin.brabocoin.model.crypto;

/**
 * Secret key used for ciphers.
 * <p>
 * Wrapper for {@link javax.crypto.SecretKey}
 */
public class SecretKey {
    private javax.crypto.SecretKey value;

    public SecretKey(javax.crypto.SecretKey value) {
        this.value = value;
    }

    public javax.crypto.SecretKey getValue() {
        return value;
    }
}
