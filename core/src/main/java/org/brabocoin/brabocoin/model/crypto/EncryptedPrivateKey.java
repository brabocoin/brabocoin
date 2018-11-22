package org.brabocoin.brabocoin.model.crypto;

public class EncryptedPrivateKey {
    private byte[] encryptedData;

    public EncryptedPrivateKey(byte[] encryptedData) {
        this.encryptedData = encryptedData;
    }
}
