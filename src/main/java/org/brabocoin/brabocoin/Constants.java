package org.brabocoin.brabocoin;

/**
 * Collection of all constants.
 */
public final class Constants {

    private Constants() {
    }

    /**
     * Fake dummy block height for {@link org.brabocoin.brabocoin.model.dal.UnspentOutputInfo}
     * entries in the transaction pool UTXO set.
     */
    public static final int TRANSACTION_POOL_HEIGHT = Integer.MAX_VALUE;

    /**
     * The amount of miniBrabos that equals one Brabocoin.
     */
    public static final long COIN = (long) 10E6;
}
