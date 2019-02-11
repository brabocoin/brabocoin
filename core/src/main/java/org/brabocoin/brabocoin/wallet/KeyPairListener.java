package org.brabocoin.brabocoin.wallet;

import org.brabocoin.brabocoin.model.crypto.KeyPair;

public interface KeyPairListener {

    /**
     * Callback for when a key pair is generated.
     *
     * @param keyPair
     *     The generated key pair.
     */
    void onKeyPairGenerated(KeyPair keyPair);
}
