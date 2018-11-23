package org.brabocoin.brabocoin.wallet;

import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.UnsignedTransaction;
import org.brabocoin.brabocoin.model.crypto.PrivateKey;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.util.Destructible;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * The wallet data structure.
 */
public class Wallet implements Iterable<Map.Entry<PublicKey, PrivateKey>> {
    /**
     * The mapping of a public key to a possibly encrypted private key.
     */
    private Map<PublicKey, PrivateKey> keyMap;

    /**
     * The wallet UTXO set.
     */
    private ReadonlyUTXOSet utxoSet;

    /**
     * Create a wallet for a given public to private key map.
     *
     * @param keyMap Key map
     */
    public Wallet(Map<PublicKey, PrivateKey> keyMap) {
        this.keyMap = keyMap;
    }

    /**
     * Get the collection of public keys.
     *
     * @return All public keys known to the wallet
     */
    public Collection<PublicKey> getPublicKeys() {
        return keyMap.keySet();
    }

    /**
     * Get the collection of private keys.
     *
     * @return All private keys known to the wallet
     */
    public Collection<PrivateKey> getPrivateKeys() {
        return keyMap.values();
    }



    /**
     * Whether the private key corresponding to the given public key is encrypted.
     *
     * @param publicKey The public key used to search the private key
     * @return Whether the matching private key is encrypted
     */
    public boolean isKeyPairEncrypted(PublicKey publicKey) {
        if (!keyMap.containsKey(publicKey)) {
            throw new IllegalStateException("Public key does not exists in keymap");
        }

        return keyMap.get(publicKey).isEncrypted();
    }

    /**
     * Sign an unsigned transaction to create a signed transaction.
     *
     * @param transaction Unsigned transaction
     * @return A signed transaction
     */
    public Transaction signTransaction(UnsignedTransaction transaction,
                                       Map<PublicKey, Destructible<char[]>> passphrases) throws DatabaseException {
        for (Input input : transaction.getInputs()) {
            UnspentOutputInfo outputInfo = utxoSet.findUnspentOutputInfo(input);
            if (outputInfo == null) {
                throw new IllegalStateException("Could not find output info for input in the transaction");
            }

            PublicKey publicKey = getPublicKeys().stream()
                    .filter(pub -> pub.getHash().equals(outputInfo.getAddress()))
                    .findFirst()
                    .orElse(null);

            if (publicKey == null) {
                throw new IllegalStateException("Referenced output uses address not found in wallet.");
            }

            PrivateKey privateKey = keyMap.get(publicKey);

            if (privateKey == null) {
                throw new IllegalStateException("Public key has null private key in keymap.");
            }

            if (!privateKey.isUnlocked()) {
                // TODO: Implement
            }
        }

        // TODO: WIP here..
        return null;
    }

    @NotNull
    @Override
    public Iterator<Map.Entry<PublicKey, PrivateKey>> iterator() {
        return keyMap.entrySet().iterator();
    }
}
