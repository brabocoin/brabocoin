package org.brabocoin.brabocoin.model.crypto;

public class SaltedSecretKey extends SecretKey {
    private final byte[] salt;

    public SaltedSecretKey(javax.crypto.SecretKey value, byte[] salt) {
        super(value);
        this.salt = salt;
    }

    public byte[] getSalt() {
        return salt;
    }
}
