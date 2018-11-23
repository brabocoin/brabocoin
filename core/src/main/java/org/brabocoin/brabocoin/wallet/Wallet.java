package org.brabocoin.brabocoin.wallet;

import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.model.crypto.PrivateKey;
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

    @NotNull
    @Override
    public Iterator<Map.Entry<PublicKey, PrivateKey>> iterator() {
        return keyMap.entrySet().iterator();
    }
}
