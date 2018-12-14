package org.brabocoin.brabocoin.wallet;

import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.crypto.KeyPair;
import org.jetbrains.annotations.Nullable;

public class TransactionSigningResult {

    private final TransactionSigningStatus status;
    private final KeyPair lockedKeyPair;
    private final Transaction transaction;

    /**
     * Private constructor for a transaction signing result.
     *
     * @param lockedKeyPair
     *     The key pair that is locked
     */
    private TransactionSigningResult(TransactionSigningStatus status,
                                     @Nullable KeyPair lockedKeyPair,
                                     @Nullable Transaction transaction) {
        this.status = status;
        this.lockedKeyPair = lockedKeyPair;
        this.transaction = transaction;
    }

    /**
     * Create a signed result.
     *
     * @return Transaction signing result
     */
    public static TransactionSigningResult signed(Transaction transaction) {
        return new TransactionSigningResult(
            TransactionSigningStatus.SIGNED,
            null,
            transaction
        );
    }

    /**
     * Create a private key locked result.
     *
     * @param lockedKeyPair
     *     The private key pair that was locked
     * @return Transaction signing result
     */
    public static TransactionSigningResult privateKeyLocked(KeyPair lockedKeyPair) {
        return new TransactionSigningResult(
            TransactionSigningStatus.PRIVATE_KEY_LOCKED,
            lockedKeyPair,
            null
        );
    }

    public TransactionSigningStatus getStatus() {
        return status;
    }

    public KeyPair getLockedKeyPair() {
        return lockedKeyPair;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
