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

    public static final int TRANSACTION_HASH_SIZE = 32;

    public static final int FORK_SWITCH_DEPTH_REORGANIZATION = 10;

    /**
     * PBKDF2
     */
    public static final int PBKDF_ITERATIONS = 10000;

    /**
     * Salt length.
     */
    public static final int SALT_LENGTH = 64;

    /**
     * JIT phantom reference enqueue timeout in seconds.
     */
    public static final int JIT_PHANTOM_ENQUEUE_TIMEOUT = 20;

    /**
     * JIT object destruction timeout in seconds.
     */
    public static final int JIT_OBJECT_DESTRUCTION_TIMEOUT = 20;

    /**
     * JIT poll delay in milliseconds.
     */
    public static final int JIT_POLL_DELAY = 5;

    /**
     * Radix of hexadecimal numbers.
     */
    public static final int HEX = 16;

    /**
     * Maximum amount of messages to store per peer.
     */
    public static final int MAX_MESSAGE_HISTORY_PER_PEER = 100;
}
