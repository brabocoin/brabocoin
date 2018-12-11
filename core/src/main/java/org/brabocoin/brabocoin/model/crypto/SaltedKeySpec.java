package org.brabocoin.brabocoin.model.crypto;

import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;

public class SaltedKeySpec extends SecretKeySpec {
    private final KeySpec keySpec;
    private final byte[] salt;

    public SaltedKeySpec(SecretKeySpec keySpec, byte[] salt) {
        super(keySpec.getEncoded(), keySpec.getAlgorithm());

        this.keySpec = keySpec;
        this.salt = salt;
    }

    public KeySpec getKeySpec() {
        return keySpec;
    }

    public byte[] getSalt() {
        return salt;
    }
}
