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

    public static final int BLOCK_HASH_SIZE = 32;
}
