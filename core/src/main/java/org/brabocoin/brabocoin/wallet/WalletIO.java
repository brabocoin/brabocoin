package org.brabocoin.brabocoin.wallet;

import org.brabocoin.brabocoin.crypto.cipher.Cipher;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.model.crypto.PrivateKey;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class WalletIO {
    private final Cipher cipher;

    public WalletIO(Cipher cipher) {
        this.cipher = cipher;
    }

    public Wallet read(File keysFile, char[] word) throws IOException, CipherException {
        if (!keysFile.exists()) {
            throw new IllegalArgumentException("File does not exists.");
        }

        byte[] rawBytes = Files.readAllBytes(keysFile.toPath());
        byte[] decryptedBytes = cipher.decyrpt(rawBytes, word);

        InputStream stream = new ByteArrayInputStream(decryptedBytes);
        PrivateKey key;

        List<PrivateKey> keyList = new ArrayList<>();
        BrabocoinProtos.PrivateKey protoKey = BrabocoinProtos.PrivateKey.parseDelimitedFrom(stream);
        while (protoKey != null) {
            key = ProtoConverter.toDomain(protoKey, PrivateKey.Builder.class);
            keyList.add(key);
            protoKey = BrabocoinProtos.PrivateKey.parseDelimitedFrom(stream);
        }

        return new Wallet(keyList);
    }

    public void write(Wallet wallet, File keysFile, char[] word) throws IOException, CipherException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (PrivateKey key : wallet) {
            BrabocoinProtos.PrivateKey protoKey = ProtoConverter.toProto(
                    key, BrabocoinProtos.PrivateKey.class
            );

            protoKey.writeDelimitedTo(outputStream);
        }

        byte[] encryptedBytes = cipher.encrypt(outputStream.toByteArray(), word);

        Files.write(keysFile.toPath(), encryptedBytes);
    }
}
