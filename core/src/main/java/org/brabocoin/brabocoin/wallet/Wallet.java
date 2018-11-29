package org.brabocoin.brabocoin.wallet;

import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.crypto.cipher.Cipher;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.UnsignedTransaction;
import org.brabocoin.brabocoin.model.crypto.KeyPair;
import org.brabocoin.brabocoin.model.crypto.PrivateKey;
import org.brabocoin.brabocoin.model.crypto.Signature;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.util.Destructible;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.wallet.generation.KeyGenerator;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The wallet data structure.
 */
public class Wallet implements Iterable<KeyPair> {

    /**
     * The public and private key collection.
     */
    private Collection<KeyPair> keyPairs;

    /**
     * Consensus used in this wallet.
     */
    private final Consensus consensus;

    /**
     * The signer used to sign transactions with.
     */
    private final Signer signer;

    /**
     * The key generator used to generate private keys.
     */
    private final KeyGenerator keyGenerator;

    /**
     * The wallet UTXO set.
     */
    private ReadonlyUTXOSet utxoSet;

    /**
     * Create a wallet for a given public to private key map.
     *
     * @param keyPairs
     *     Key map
     */
    public Wallet(Collection<KeyPair> keyPairs, Consensus consensus, Signer signer,
                  KeyGenerator keyGenerator) {
        this.keyPairs = keyPairs;
        this.consensus = consensus;
        this.signer = signer;
        this.keyGenerator = keyGenerator;
    }

    /**
     * Get the collection of public keys.
     *
     * @return All public keys known to the wallet
     */
    public Collection<PublicKey> getPublicKeys() {
        return keyPairs.stream().map(KeyPair::getPublicKey).collect(Collectors.toList());
    }

    /**
     * Get the collection of private keys.
     *
     * @return All private keys known to the wallet
     */
    public Collection<PrivateKey> getPrivateKeys() {
        return keyPairs.stream().map(KeyPair::getPrivateKey).collect(Collectors.toList());
    }

    /**
     * Finds a key pair.
     *
     * @param publicKey
     *     The public key to find the key pair for
     * @return The key pair or null if not found
     */
    public KeyPair findKeyPair(PublicKey publicKey) {
        return keyPairs.stream().filter(p -> p.getPublicKey() == publicKey)
            .findFirst()
            .orElse(null);
    }

    /**
     * Find the private key for a public key.
     *
     * @param publicKey
     *     The public key to find the private key for
     * @return Private key or null if not found
     */
    public PrivateKey findPrivateKey(PublicKey publicKey) {
        return Optional.ofNullable(findKeyPair(publicKey)).map(KeyPair::getPrivateKey).orElse(null);
    }

    /**
     * Whether the private key corresponding to the given public key is encrypted.
     *
     * @param publicKey
     *     The public key used to search the private key
     * @return Whether the matching private key is encrypted
     */
    public boolean isKeyPairEncrypted(PublicKey publicKey) {
        if (!getPublicKeys().contains(publicKey)) {
            throw new IllegalStateException("Public key does not exists in keymap");
        }

        return findKeyPair(publicKey).getPrivateKey().isEncrypted();
    }

    /**
     * Attempts to sign an unsigned transaction.
     * <p>
     * When successful, return a {@link TransactionSigningResult} with the resulting signed
     * transaction. When unsuccessful, return a {@link TransactionSigningResult} that contains
     * the key pair that was locked.
     *
     * @param unsignedTransaction
     *     Unsigned transaction
     * @return A transaction signing result
     */
    public TransactionSigningResult signTransaction(
        UnsignedTransaction unsignedTransaction) throws DatabaseException, DestructionException {
        List<Signature> signatures = new ArrayList<>();

        for (Input input : unsignedTransaction.getInputs()) {
            UnspentOutputInfo outputInfo = utxoSet.findUnspentOutputInfo(input);
            if (outputInfo == null) {
                throw new IllegalStateException(
                    "Could not find output info for input in the transaction");
            }

            PublicKey publicKey = getPublicKeys().stream()
                .filter(pub -> pub.getHash().equals(outputInfo.getAddress()))
                .findFirst()
                .orElse(null);

            if (publicKey == null) {
                throw new IllegalStateException(
                    "Referenced output uses address not found in wallet.");
            }

            KeyPair keyPair = findKeyPair(publicKey);

            if (keyPair == null) {
                throw new IllegalStateException(
                    "Could not find key pair belonging to public key.");
            }

            PrivateKey privateKey = keyPair.getPrivateKey();

            if (!privateKey.isUnlocked()) {
                return TransactionSigningResult.privateKeyLocked(keyPair);
            }

            Destructible<BigInteger> privateKeyValue = privateKey.getKey();
            signatures.add(
                signer.signMessage(
                    unsignedTransaction.getSignableTransactionData(),
                    Objects.requireNonNull(privateKeyValue.getReference().get())
                )
            );
            privateKeyValue.destruct();
        }


        return TransactionSigningResult.signed(
            unsignedTransaction.sign(signatures)
        );
    }

    /**
     * Generates and adds a plain key pair to the wallet.
     *
     * @return Key pair that was generated.
     */
    public KeyPair generatePlainKeyPair() throws DestructionException {
        Destructible<BigInteger> randomBigInteger = keyGenerator.generateKey(consensus.getCurve()
            .getDomain()
            .getN());
        PrivateKey privateKey = PrivateKey.plain(randomBigInteger.getReference().get());
        PublicKey publicKey = consensus.getCurve().getPublicKeyFromPrivateKey(
            Objects.requireNonNull(randomBigInteger.getReference().get())
        );

        randomBigInteger.destruct();

        KeyPair keyPair = new KeyPair(publicKey, privateKey);
        keyPairs.add(keyPair);

        return keyPair;
    }

    /**
     * Generates and adds an encrypted key pair to the wallet.
     *
     * @param passphrase
     *     The passphrase to encrypt the private key with
     * @param cipher
     *     The cipher used to encrypt the private key with
     * @return Key pair that was generated.
     */
    public KeyPair generateEncryptedKeyPair(Destructible<char[]> passphrase,
                                            Cipher cipher) throws DestructionException, CipherException {
        Destructible<BigInteger> randomBigInteger = keyGenerator.generateKey(consensus.getCurve()
            .getDomain()
            .getN());
        // Also destructs the passphrase and big integer
        PrivateKey privateKey = PrivateKey.encrypted(randomBigInteger, passphrase, cipher);
        PublicKey publicKey = consensus.getCurve().getPublicKeyFromPrivateKey(
            Objects.requireNonNull(randomBigInteger.getReference().get())
        );

        KeyPair keyPair = new KeyPair(publicKey, privateKey);
        keyPairs.add(keyPair);

        return keyPair;
    }

    @NotNull
    @Override
    public Iterator<KeyPair> iterator() {
        return keyPairs.iterator();
    }
}
