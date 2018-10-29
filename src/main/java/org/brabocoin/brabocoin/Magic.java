package org.brabocoin.brabocoin;

/**
 * Collection of all magic constants.
 */
public final class Magic {

    private Magic() {
    }

    /**
     * Fake dummy block height for {@link org.brabocoin.brabocoin.model.dal.UnspentOutputInfo}
     * entries in the transaction pool UTXO set.
     */
    public static final int TRANSACTION_POOL_HEIGHT = Integer.MAX_VALUE;
}
