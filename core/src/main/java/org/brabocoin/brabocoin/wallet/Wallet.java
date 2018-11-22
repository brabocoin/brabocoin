package org.brabocoin.brabocoin.wallet;

import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.model.crypto.PrivateKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * The wallet data structure.
 */
public class Wallet implements Iterable<Map.Entry<PublicKey, PrivateKey>> {
    private Map<PublicKey, PrivateKey> keyMap;

    public Wallet(Map<PublicKey, PrivateKey> keyMap) {
        this.keyMap = keyMap;
    }

    public Collection<PublicKey> getPublicKeys() {
        return keyMap.keySet();
    }

    public Collection<PrivateKey> getPrivateKeys() {
        return keyMap.values();
    }

    @NotNull
    @Override
    public Iterator<Map.Entry<PublicKey, PrivateKey>> iterator() {
        return keyMap.entrySet().iterator();
    }
}
