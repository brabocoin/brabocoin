package org.brabocoin.brabocoin.crypto.cipher;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.model.crypto.SaltedKeySpec;
import org.brabocoin.brabocoin.model.crypto.SaltedSecretKey;
import org.brabocoin.brabocoin.util.CryptoUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public final class BouncyCastleAES extends KeyCipher {
    private static final int keyLength = 256;
    private static final Provider provider = new BouncyCastleProvider();
    private static javax.crypto.Cipher cipher;


    public BouncyCastleAES() throws CipherException {
        if (cipher != null) {
            return;
        }

        try {
            cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding", new BouncyCastleProvider());
        } catch (NoSuchAlgorithmException e) {
            throw new CipherException("Algorithm not found", e);
        } catch (NoSuchPaddingException e) {
            throw new CipherException("Padding method not found", e);
        }
    }

    @Override
    public byte[] encrypt(byte[] input, char[] word) throws CipherException {
        SaltedKeySpec spec = createKeySpec(createSecretKey(word));
        byte[] iv = CryptoUtil.generateBytes(cipher.getBlockSize());
        try {
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, spec, new IvParameterSpec(iv));
        } catch (InvalidKeyException e) {
            throw new CipherException("Key is invalid", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new CipherException("Invalid algorithm parameter", e);
        }

        byte[] output;

        try {
            output = cipher.doFinal(input);
        } catch (IllegalBlockSizeException e) {
            throw new CipherException("Illegal block size", e);
        } catch (BadPaddingException e) {
            throw new CipherException("Padding failed", e);
        }

        return Arrays.concatenate(spec.getSalt(), iv, output);
    }

    @Override
    public byte[] decyrpt(byte[] input, char[] word) throws CipherException {
        byte[] salt = Arrays.copyOf(input, Constants.SALT_LENGTH);
        byte[] iv = Arrays.copyOfRange(input, Constants.SALT_LENGTH, Constants.SALT_LENGTH + cipher.getBlockSize());
        byte[] data = Arrays.copyOfRange(input, Constants.SALT_LENGTH + cipher.getBlockSize(), input.length);
        try {
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE,
                    createKeySpec(createSecretKey(word, salt)),
                    new IvParameterSpec(iv));
        } catch (InvalidKeyException e) {
            throw new CipherException("Key is invalid", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new CipherException("Invalid algorithm parameter", e);
        }

        byte[] output;

        try {
            output = cipher.doFinal(data);
        } catch (IllegalBlockSizeException e) {
            throw new CipherException("Illegal block size", e);
        } catch (BadPaddingException e) {
            throw new CipherException("Padding failed", e);
        }

        return output;
    }

    @Override
    public SaltedSecretKey createSecretKey(char[] word) throws CipherException {
        return createSecretKey(word, CryptoUtil.generateBytes(Constants.SALT_LENGTH));
    }

    @Override
    public SaltedSecretKey createSecretKey(char[] word, byte[] salt) throws CipherException {
        KeySpec pbeKeySpec = new PBEKeySpec(word,
                salt,
                Constants.PBKDF_ITERATIONS,
                keyLength);
        SecretKeyFactory keyFac;
        try {
            keyFac = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256", provider);
        } catch (NoSuchAlgorithmException e) {
            throw new CipherException("Could not find secret key factory algorithm", e);
        }

        javax.crypto.SecretKey pbeKey;
        try {
            pbeKey = keyFac.generateSecret(pbeKeySpec);
        } catch (InvalidKeySpecException e) {
            throw new CipherException("Invalid key spec for secret generation");
        }

        return new SaltedSecretKey(pbeKey, salt);
    }

    @Override
    SaltedKeySpec createKeySpec(SaltedSecretKey key) throws CipherException {
        return new SaltedKeySpec(
                new SecretKeySpec(key.getValue().getEncoded(), "AES"),
                key.getSalt());
    }
}
