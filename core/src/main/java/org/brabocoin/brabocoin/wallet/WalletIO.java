package org.brabocoin.brabocoin.wallet;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.crypto.cipher.Cipher;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.crypto.PrivateKey;
import org.brabocoin.brabocoin.model.proto.Secp256k1PublicKeyByteStringConverter;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.Destructible;
import org.brabocoin.brabocoin.util.ProtoConverter;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class WalletIO {
    private final Cipher cipher;
    private Secp256k1PublicKeyByteStringConverter converter = new Secp256k1PublicKeyByteStringConverter();

    public WalletIO(Cipher cipher) {
        this.cipher = cipher;
    }

    public Wallet read(File keysFile, Destructible<char[]> passphrase) throws IOException, CipherException, DestructionException {
        if (!keysFile.exists()) {
            throw new IllegalArgumentException("File does not exists.");
        }

        byte[] rawBytes = Files.readAllBytes(keysFile.toPath());
        byte[] decryptedBytes = cipher.decyrpt(rawBytes, passphrase.getReference().get());

        InputStream stream = new ByteArrayInputStream(decryptedBytes);
        PublicKey publicKey;
        Hash publicKeyHash;
        PrivateKey privateKey;

        Map<PublicKey, PrivateKey> keyList = new HashMap<>();

        BrabocoinProtos.Hash protoPubHash = BrabocoinProtos.Hash.parseDelimitedFrom(stream);
        BrabocoinProtos.PrivateKey protoKey = BrabocoinProtos.PrivateKey.parseDelimitedFrom(stream);

        while (protoPubHash != null && protoKey != null) {
            publicKeyHash = ProtoConverter.toDomain(protoPubHash, Hash.Builder.class);
            if (publicKeyHash == null) {
                throw new IllegalStateException("Could not decode hash of public key.");
            }
            publicKey = converter.toDomainValue(publicKeyHash.getValue());
            privateKey = ProtoConverter.toDomain(protoKey, PrivateKey.Builder.class);
            keyList.put(publicKey, privateKey);

            protoPubHash = BrabocoinProtos.Hash.parseDelimitedFrom(stream);
            protoKey = BrabocoinProtos.PrivateKey.parseDelimitedFrom(stream);
        }

        passphrase.destruct();

        return new Wallet(keyList);
    }

    public void write(Wallet wallet, File keysFile, Destructible<char[]> passphrase) throws IOException, CipherException, DestructionException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (Map.Entry<PublicKey, PrivateKey> entry : wallet) {
            ByteString pubKey = entry.getKey().toCompressed();
            BrabocoinProtos.Hash protoPubHash = ProtoConverter.toProto(new Hash(pubKey), BrabocoinProtos.Hash.class);
            BrabocoinProtos.PrivateKey protoKey = ProtoConverter.toProto(
                    entry.getValue(), BrabocoinProtos.PrivateKey.class
            );

            protoPubHash.writeDelimitedTo(outputStream);
            protoKey.writeDelimitedTo(outputStream);
        }

        byte[] encryptedBytes = cipher.encrypt(outputStream.toByteArray(), passphrase.getReference().get());

        passphrase.destruct();

        Files.write(keysFile.toPath(), encryptedBytes);
    }
}
