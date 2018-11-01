package org.brabocoin.brabocoin.validation.transaction;

import org.brabocoin.brabocoin.model.Transaction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Validation rules for transactions.
 */
public class TransactionValidator {
    private static final Logger LOGGER = Logger.getLogger(TransactionValidator.class.getName());

    private List<Function<Transaction, Boolean>> rules = new ArrayList<>();

    /**
     * Checks whether a transaction is valid.
     *
     * @param transaction
     *     The transaction.
     * @return Whether the transaction is valid.
     */
    public boolean checkTransactionValid(@NotNull Transaction transaction) {
        for (Function<Transaction, Boolean> rule : rules) {
            if (!rule.apply(transaction)) {
                return false;
            }
        }

        return true;
    }

}
