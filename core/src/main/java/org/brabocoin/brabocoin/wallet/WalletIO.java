package org.brabocoin.brabocoin.wallet;

import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.crypto.cipher.Cipher;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.model.crypto.KeyPair;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.Destructible;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.wallet.generation.KeyGenerator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class WalletIO {

    private final Cipher cipher;

    public WalletIO(Cipher cipher) {
        this.cipher = cipher;
    }

    public Wallet read(File keysFile, Destructible<char[]> passphrase, Consensus consensus,
                       Signer signer,
                       KeyGenerator keyGenerator,
                       Cipher privateKeyCipher) throws IOException, CipherException,
                                                       DestructionException {
        if (!keysFile.exists()) {
            throw new IllegalArgumentException("File does not exists.");
        }

        byte[] rawBytes = Files.readAllBytes(keysFile.toPath());
        byte[] decryptedBytes = cipher.decyrpt(rawBytes, passphrase.getReference().get());

        InputStream stream = new ByteArrayInputStream(decryptedBytes);

        List<KeyPair> keyList = new ArrayList<>();

        KeyPair keyPair;
        BrabocoinProtos.KeyPair protoKeyPair = BrabocoinProtos.KeyPair.parseDelimitedFrom(stream);

        while (protoKeyPair != null) {
            keyPair = ProtoConverter.toDomain(protoKeyPair, KeyPair.Builder.class);
            if (keyPair == null) {
                throw new IllegalStateException("Could not parse KeyPair from read wallet file");
            }
            keyList.add(keyPair);

            protoKeyPair = BrabocoinProtos.KeyPair.parseDelimitedFrom(stream);
        }

        passphrase.destruct();

        return new Wallet(keyList, consensus, signer, keyGenerator, privateKeyCipher);
    }

    public void write(Wallet wallet, File keysFile,
                      Destructible<char[]> passphrase) throws IOException, CipherException,
                                                              DestructionException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (KeyPair keyPair : wallet) {
            BrabocoinProtos.KeyPair protoKeyPair = ProtoConverter.toProto(
                keyPair,
                BrabocoinProtos.KeyPair.class
            );

            protoKeyPair.writeDelimitedTo(outputStream);
        }

        byte[] encryptedBytes = cipher.encrypt(
            outputStream.toByteArray(),
            passphrase.getReference().get()
        );

        passphrase.destruct();

        Files.write(keysFile.toPath(), encryptedBytes);
    }
}
