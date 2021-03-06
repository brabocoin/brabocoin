package org.brabocoin.brabocoin.processor;

import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.ValidationStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result of processing a new transaction.
 */
public class ProcessedTransactionResult {

    /**
     * The status of the newly added transaction.
     */
    private final @NotNull ValidationStatus status;

    /**
     * List of any orphans that are added to the transaction pool (as dependent transactions) as
     * a result of the new transaction being present in the transaction pool.
     */
    private final @NotNull List<Transaction> addedOrphans;

    /**
     * Create a new processed transaction result.
     *
     * @param status
     *     The status of the newly added transaction.
     * @param addedOrphans
     *     ist of any orphans that are added to the transaction pool (as dependent transactions)
     *     as a result of the new transaction being present in the transaction pool.
     */
    public ProcessedTransactionResult(@NotNull ValidationStatus status,
                                      @NotNull List<Transaction> addedOrphans) {
        this.status = status;
        this.addedOrphans = new ArrayList<>(addedOrphans);
    }

    /**
     * The status of the newly added transaction.
     *
     * @return The status of the newly added transaction.
     */
    public ValidationStatus getStatus() {
        return status;
    }

    /**
     * List of any orphans that are added to the transaction pool (as dependent transactions) as
     * a result of the new transaction being present in the transaction pool.
     *
     * @return List of orphans.
     */
    public List<Transaction> getValidatedOrphans() {
        return Collections.unmodifiableList(addedOrphans);
    }
}
