package org.brabocoin.brabocoin.validation;

import org.brabocoin.brabocoin.model.Transaction;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Validation rules for transactions.
 */
public class TransactionValidator {

    private static final Logger LOGGER = Logger.getLogger(TransactionValidator.class.getName());

    /**
     * Checks whether a transaction is valid.
     *
     * @param transaction
     *     The transaction.
     * @return Whether the transaction is valid.
     */
    public boolean checkTransactionValid(@NotNull Transaction transaction) {
        // TODO: implement
        return true;
    }
}
