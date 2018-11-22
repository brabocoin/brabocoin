package org.brabocoin.brabocoin.wallet;

import org.brabocoin.brabocoin.model.crypto.PrivateKey;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

/**
 * The wallet data structure.
 */
public class Wallet implements Iterable<PrivateKey> {
    private List<PrivateKey> privateKeys;

    public Wallet(List<PrivateKey> privateKeys) {
        this.privateKeys = privateKeys;
    }

    @NotNull
    @Override
    public Iterator<PrivateKey> iterator() {
        return privateKeys.iterator();
    }
}
