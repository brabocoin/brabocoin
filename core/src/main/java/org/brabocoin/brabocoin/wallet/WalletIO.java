package org.brabocoin.brabocoin.wallet;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.crypto.cipher.Cipher;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.dal.UTXODatabase;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.model.crypto.KeyPair;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.Destructible;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.wallet.generation.KeyGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WalletIO {

    private final Cipher cipher;

    public WalletIO(Cipher cipher) {
        this.cipher = cipher;
    }

    public Wallet read(File keysFile, File transactionHistoryFile, Destructible<char[]> passphrase,
                       Consensus consensus,
                       Signer signer,
                       KeyGenerator keyGenerator,
                       Cipher privateKeyCipher,
                       @NotNull UTXODatabase walletUTXOSet,
                       @NotNull ReadonlyUTXOSet watchedUTXOSet,
                       @NotNull Blockchain blockchain) throws IOException, CipherException,
                       DestructionException {
        if (!keysFile.exists()) {
            throw new IllegalArgumentException("Keys file does not exist.");
        }

        if (!transactionHistoryFile.exists()) {
            throw new IllegalArgumentException("Transaction history does not exist.");
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


        // Load transaction history
        byte[] txHistoryBytes = Files.readAllBytes(transactionHistoryFile.toPath());
        TransactionHistory transactionHistory;
        if (txHistoryBytes.length == 0) {
            transactionHistory = new TransactionHistory(
                Collections.emptyMap(),
                Collections.emptyMap()
            );
        }
        else {
            BrabocoinStorageProtos.TransactionHistory protoTxHistory = BrabocoinStorageProtos.TransactionHistory.parseFrom(
                txHistoryBytes
            );
            transactionHistory = ProtoConverter.toDomain(protoTxHistory, TransactionHistory.Builder.class);
            if (transactionHistory == null) {
                throw new IllegalStateException("Transaction history could not be read.");
            }
        }

        return new Wallet(
            keyList,
            transactionHistory,
            consensus,
            signer,
            keyGenerator,
            privateKeyCipher,
            walletUTXOSet,
            watchedUTXOSet,
            blockchain
        );
    }

    public void write(Wallet wallet, File keysFile, File transactionHistoryFile,
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

        // Write transaction history
        if (!wallet.getTransactionHistory().isEmpty()) {
            ByteString txHistoryBytes = ProtoConverter.toProtoBytes(
                wallet.getTransactionHistory(),
                BrabocoinStorageProtos.TransactionHistory.class
            );

            if (txHistoryBytes == null) {
                throw new IllegalStateException("Transaction history could not be saved.");
            }

            Files.write(transactionHistoryFile.toPath(), txHistoryBytes.toByteArray());
        }
        else {
            Files.write(transactionHistoryFile.toPath(), new byte[] {});
        }
    }
}
