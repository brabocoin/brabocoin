package org.brabocoin.brabocoin.wallet;

import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.UnsignedTransaction;
import org.brabocoin.brabocoin.model.crypto.KeyPair;
import org.brabocoin.brabocoin.model.crypto.PrivateKey;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.util.Destructible;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
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
     * The wallet UTXO set.
     */
    private ReadonlyUTXOSet utxoSet;

    /**
     * Create a wallet for a given public to private key map.
     *
     * @param keyPairs
     *     Key map
     */
    public Wallet(Collection<KeyPair> keyPairs) {
        this.keyPairs = keyPairs;
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
     * Sign an unsigned transaction to create a signed transaction.
     *
     * @param transaction
     *     Unsigned transaction
     * @return A signed transaction
     */
    public Transaction signTransaction(UnsignedTransaction transaction,
                                       Map<PublicKey, Destructible<char[]>> passphrases) throws DatabaseException {
        for (Input input : transaction.getInputs()) {
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

            if (!keyPair.getPrivateKey().isUnlocked()) {
                // TODO: Implement
            }
        }

        // TODO: WIP here..
        return null;
    }

    @NotNull
    @Override
    public Iterator<KeyPair> iterator() {
        return keyPairs.iterator();
    }
}
