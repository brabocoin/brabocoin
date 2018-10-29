package org.brabocoin.brabocoin.dal;

import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.util.collection.MultiDependenceIndex;
import org.brabocoin.brabocoin.util.collection.RecursiveMultiDependenceIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Random;
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
     * The random instance.
     */
    private final Random random;

    /**
     * Maximum number of transactions in the orphan pool.
     */
    private final int maxOrphanPoolSize;

    /**
     * Maximum number of dependent and independent transactions (combined).
     */
    private final int maxPoolSize;

    /**
     * Index of independent transactions, indexed by the transaction hash and the transactions hash
     * of all inputs.
     */
    private final MultiDependenceIndex<Hash, Transaction, Hash> independentTransactions;

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
     * @param random
     *     The random instance.
     */
    public TransactionPool(@NotNull BraboConfig config, @NotNull Random random) {
        LOGGER.info("Initializing transaction pool.");

        this.maxOrphanPoolSize = config.maxOrphanTransactions();
        this.maxPoolSize = config.maxTransactionPoolSize();
        this.random = random;

        this.independentTransactions = new MultiDependenceIndex<>(Transaction::computeHash,
            transaction -> transaction.getInputs()
                .stream()
                .map(Input::getReferencedTransaction)
                .collect(Collectors.toSet())
        );

        this.dependentTransactions = new RecursiveMultiDependenceIndex<>(Transaction::computeHash,
            transaction -> transaction.getInputs()
                .stream()
                .map(Input::getReferencedTransaction)
                .collect(Collectors.toSet()),
            Transaction::computeHash
        );

        this.orphanTransactions = new RecursiveMultiDependenceIndex<>(Transaction::computeHash,
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
     * When the transaction was previously stored as orphan transaction, it is removed from the
     * orphan set.
     *
     * @param transaction
     *     The validated transaction to be stored as independent in the transaction pool.
     */
    public void addIndependentTransaction(@NotNull Transaction transaction) {
        LOGGER.fine("Adding independent transaction.");
        orphanTransactions.removeValue(transaction);
        independentTransactions.put(transaction);

        limitTransactionPoolSize();
    }

    /**
     * Add an dependent transaction to the transaction pool.
     * <p>
     * An dependent transaction has some dependencies on other transactions in the pool.
     * <p>
     * Note: the supplied transaction must be validated before being added to the transaction pool.
     * When the transaction was previously stored as orphan transaction, it is removed from the
     * orphan set.
     *
     * @param transaction
     *     The validated transaction to be stored as dependent in the transaction pool.
     */
    public void addDependentTransaction(@NotNull Transaction transaction) {
        LOGGER.fine("Adding dependent transaction.");
        orphanTransactions.removeValue(transaction);
        dependentTransactions.put(transaction);
    }

    /**
     * Limit the size of the transaction pool.
     */
    public void limitTransactionPoolSize() {
        LOGGER.fine("Limiting the size of the transaction pool.");
        // Remove dependent transactions first
        while (dependentTransactions.size() + independentTransactions.size() > maxPoolSize) {
            if (dependentTransactions.size() > 0) {
                Hash remove = dependentTransactions.getKeyAt(random.nextInt(dependentTransactions.size()));
                dependentTransactions.removeKey(remove);
                List<Transaction> removed = dependentTransactions.removeMatchingDependants(remove, t -> true);

                LOGGER.finest(() -> MessageFormat.format("Removed {0} dependent transactions from the pool.", removed.size() + 1));
            }
            else {
                Hash remove = independentTransactions.getKeyAt(random.nextInt(independentTransactions.size()));
                independentTransactions.removeKey(remove);
                LOGGER.finest("Removed an independent transactions from the pool.");
            }
        }
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

        // Remove first orphan when size limit is reached
        while (orphanTransactions.size() > maxOrphanPoolSize) {
            Hash remove = orphanTransactions.getKeyAt(random.nextInt(orphanTransactions.size()));
            orphanTransactions.removeKey(remove);
            LOGGER.finest("Removed orphan transaction to limit max size of the pool.");
        }
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
     * Remove the transaction from the transaction pool.
     * <p>
     * Only transactions from the independent and dependent set are searched.
     *
     * @param hash
     *     The hash of the transaction.
     */
    public void removeValidatedTransaction(Hash hash) {
        LOGGER.fine("Removing validated transaction from the transaction pool.");
        independentTransactions.removeKey(hash);
        dependentTransactions.removeKey(hash);
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
     * Promote all dependent transactions descending from the given parent when the dependent
     * transaction recursively match {@code matcher}.
     * <p>
     * Matching transactions are removed from the dependent set and added to the independent set.
     *
     * @param parentHash
     *     The hash of the parent.
     * @param matcher
     *     Function indicating whether the dependent transaction can be removed.
     */
    public void promoteDependentToIndependentFromParent(@NotNull Hash parentHash,
                                                        @NotNull Function<Transaction, Boolean> matcher) {
        List<Transaction> transactions = dependentTransactions.removeMatchingDependants(parentHash,
            t -> {
                if (matcher.apply(t)) {
                    independentTransactions.put(t);
                    return true;
                }
                return false;
            }
        );

        LOGGER.fine(() -> MessageFormat.format(
            "Promoted {0} transactions to independent.",
            transactions.size()
        ));
    }

    /**
     * Demote all independent transactions that depend on {@code dependency} to dependent.
     *
     * @param dependency
     *     The dependency.
     */
    public void demoteIndependentToDependent(@NotNull Hash dependency) {
        Collection<Transaction> transactions = independentTransactions.getFromDependency(dependency);

        for (Transaction transaction : transactions) {
            independentTransactions.removeValue(transaction);
            dependentTransactions.put(transaction);
        }

        LOGGER.fine(() -> MessageFormat.format(
            "Demoted {0} transactions from dependent to independent.",
            transactions.size()
        ));
    }

    /**
     * Checks whether the transaction is known to the transaction pool.
     *
     * @param hash
     *     The hash of the transaction.
     * @return Whether the transaction is known as validated.
     */
    public boolean hasValidTransaction(@NotNull Hash hash) {
        LOGGER.fine("Checking if valid transaction is known.");
        return isIndependent(hash) || isDependent(hash);
    }

    /**
     * Checks whether the transaction is known as orphan.
     *
     * @param hash
     *     The hash of the transaction.
     * @return Whether the transaction is known as orphan.
     */
    public boolean isOrphan(@NotNull Hash hash) {
        LOGGER.fine("Checking if orphan is known.");
        return orphanTransactions.containsKey(hash);
    }

    /**
     * Checks whether the transaction is known as dependent.
     *
     * @param hash
     *     The hash of the transaction.
     * @return Whether the transaction is known as dependent.
     */
    public boolean isDependent(@NotNull Hash hash) {
        return dependentTransactions.containsKey(hash);
    }

    /**
     * Checks whether the transaction is known as independent.
     *
     * @param hash
     *     The hash of the transaction.
     * @return Whether the transaction is known as independent.
     */
    public boolean isIndependent(@NotNull Hash hash) {
        return independentTransactions.containsKey(hash);
    }
}
