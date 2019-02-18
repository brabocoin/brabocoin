package org.brabocoin.brabocoin.model;

import org.brabocoin.brabocoin.validation.transaction.TransactionValidationResult;
import org.jetbrains.annotations.NotNull;

/**
 * A transaction that has been rejected.
 */
public class RejectedTransaction {

    private final @NotNull Transaction transaction;

    private final @NotNull TransactionValidationResult validationResult;

    public RejectedTransaction(@NotNull Transaction transaction,
                               @NotNull TransactionValidationResult validationResult) {
        this.transaction = transaction;
        this.validationResult = validationResult;
    }

    public @NotNull Transaction getTransaction() {
        return transaction;
    }

    public @NotNull TransactionValidationResult getValidationResult() {
        return validationResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RejectedTransaction that = (RejectedTransaction)o;

        return transaction.getHash().equals(that.transaction.getHash());
    }

    @Override
    public int hashCode() {
        return transaction.getHash().hashCode();
    }
}
