package org.brabocoin.brabocoin.dal;

import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.util.collection.RecursiveMultiDependenceIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Pool of transactions not (yet) recorded in a block on the validated main chain in the blockchain.
 * <p>
 * Maintains three types of unconfirmed transactions:
 * <ul>
 * <li><em>Independent</em>: all inputs of the transaction reference an UTXO already confirmed in
 * the main chain.</li>
 * <li><em>Dependent</em>: some inputs reference an UTXO from the transaction pool (from an
 * independent or dependent transaction).</li>
 * <li><em>Orphan</em>: some inputs reference an output not in either UTXO set.</li>
 * </ul>
 * <p>
 * The order of precedence is <em>orphan, dependent, independent</em>. The status of an
 * transaction can elevate in this order of precedence.
 * <p>
 * Orphan transactions are not considered to be part of the transaction pool and are not
 * validated to be included in a block, to prevent double spending. The size of the orphan set is
 * limited and entries have an expiration time.
 * When the limit is met, orphan transactions are discarded randomly.
 */
public class TransactionPool {

    private final static Logger LOGGER = Logger.getLogger(TransactionPool.class.getName());

    /**
     * Index of independent transactions, indexed by the transaction hash.
     */
    private final Map<Hash, Transaction> independentTransactions;

    /**
     * Index of dependent transactions, indexed by their hash and the transaction hash of all
     * inputs.
     */
    private final RecursiveMultiDependenceIndex<Hash, Transaction, Hash> dependentTransactions;

    /**
     * Index of orphan transactions, indexed by their hash and the transaction hash of all
     * inputs.
     */
    private final RecursiveMultiDependenceIndex<Hash, Transaction, Hash> orphanTransactions;

    /**
     * Creates an empty transaction pool.
     *
     * @param config
     *     The configuration.
     */
    public TransactionPool(@NotNull BraboConfig config) {
        LOGGER.info("Initializing transaction pool.");

        this.independentTransactions = new HashMap<>();

        this.dependentTransactions = new RecursiveMultiDependenceIndex<>(
            Transaction::computeHash,
            transaction -> transaction.getInputs()
                .stream()
                .map(Input::getReferencedTransaction)
                .collect(Collectors.toSet()),
            Transaction::computeHash
        );

        this.orphanTransactions = new RecursiveMultiDependenceIndex<>(
            Transaction::computeHash,
            transaction -> transaction.getInputs()
                .stream()
                .map(Input::getReferencedTransaction)
                .collect(Collectors.toSet()),
            Transaction::computeHash
        );
    }

    /**
     * Add an independent transaction to the transaction pool.
     * <p>
     * An independent transaction has no dependencies on any other transactions that are not
     * recorded in the blockchain.
     * <p>
     * Note: the supplied transaction must be validated before being added to the transaction pool.
     *
     * @param transaction
     *     The validated transaction to be stored as independent in the transaction pool.
     */
    public void addIndependentTransaction(@NotNull Transaction transaction) {
        LOGGER.fine("Adding independent transaction.");
        independentTransactions.put(transaction.computeHash(), transaction);
    }

    /**
     * Add an dependent transaction to the transaction pool.
     * <p>
     * An dependent transaction has some dependencies on other transactions in the pool.
     * <p>
     * Note: the supplied transaction must be validated before being added to the transaction pool.
     *
     * @param transaction
     *     The validated transaction to be stored as dependent in the transaction pool.
     */
    public void addDependentTransaction(@NotNull Transaction transaction) {
        LOGGER.fine("Adding dependent transaction.");
        dependentTransactions.put(transaction);
    }

    /**
     * Add an orphan transaction to the transaction pool.
     * <p>
     * An orphan transaction has some dependencies on outputs that are not known to be unspent.
     *
     * @param transaction
     *     The transaction to be stored as orphan in the transaction pool.
     */
    public void addOrphanTransaction(@NotNull Transaction transaction) {
        LOGGER.fine("Adding orphan transaction.");
        orphanTransactions.put(transaction);
    }

    /**
     * Find the transaction by hash in the transaction pool.
     * <p>
     * Only transactions from the independent and dependent set are searched. Orphan transactions
     * are not validated.
     *
     * @param transactionHash
     *     The hash of the transaction.
     * @return The transaction with the given hash, or {@code null} if no such transaction can be
     * found.
     */
    public @Nullable Transaction findValidatedTransaction(@NotNull Hash transactionHash) {
        LOGGER.fine("Finding validated transaction.");
        if (independentTransactions.containsKey(transactionHash)) {
            LOGGER.fine("Transaction found in independent set.");
            return independentTransactions.get(transactionHash);
        }

        LOGGER.fine("Attempt to find transaction in dependent set.");
        return dependentTransactions.get(transactionHash);
    }

    /**
     * Find an orphan transaction.
     *
     * @param transactionHash
     *     The hash of the transaction.
     * @return The transaction with the given hash, or {@code null} if no such transaction can be
     * found from the orphan pool.
     */
    public @Nullable Transaction findOrphan(@NotNull Hash transactionHash) {
        LOGGER.fine("Attempt to find orphan transaction.");
        return orphanTransactions.get(transactionHash);
    }

    /**
     * Remove all orphans descending from the given parent when the orphan is valid according to
     * {@code orphanValidator}.
     *
     * @param parentHash
     *     The hash of the parent.
     * @param orphanValidator
     *     Function validating whether the orphan can be removed.
     * @return The list of removed orphans.
     */
    public @NotNull List<Transaction> removeValidOrphansFromParent(@NotNull Hash parentHash,
                                                                   @NotNull Function<Transaction,
                                                                       Boolean> orphanValidator) {
        return orphanTransactions.removeMatchingDependants(parentHash, orphanValidator);
    }

    /**
     * Checks whether the transaction is known to the transaction pool, either as validated or
     * orphan transaction.
     *
     * @param hash
     *     The hash of the transaction.
     * @return Whether the transaction is known.
     */
    public boolean isHashKnown(@NotNull Hash hash) {
        LOGGER.fine("Checking if hash is known.");
        return independentTransactions.containsKey(hash)
            || dependentTransactions.containsKey(hash)
            || orphanTransactions.containsKey(hash);
    }
}
