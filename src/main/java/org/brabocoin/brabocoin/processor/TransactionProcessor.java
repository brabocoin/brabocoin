package org.brabocoin.brabocoin.processor;

import org.brabocoin.brabocoin.Magic;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.dal.UTXODatabase;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.TransactionValidator;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Logger;

/**
 * Processes transactions to and from the {@link org.brabocoin.brabocoin.dal.TransactionPool}.
 */
public class TransactionProcessor {

    private static final Logger LOGGER = Logger.getLogger(TransactionProcessor.class.getName());

    /**
     * Transaction validator.
     */
    private final @NotNull TransactionValidator transactionValidator;

    /**
     * The transaction pool.
     */
    private final @NotNull TransactionPool transactionPool;

    /**
     * The UTXO database of the blockchain.
     */
    private final @NotNull ChainUTXODatabase utxoFromChain;

    /**
     * The UTXO database of the transaction pool.
     */
    private final @NotNull UTXODatabase utxoFromPool;

    /**
     * Create a new transaction processor.
     *
     * @param transactionValidator
     *     The transaction validator.
     * @param transactionPool
     *     The transaction pool.
     * @param utxoFromChain
     *     The UTXO database of the blockchain.
     * @param utxoFromPool
     *     The UTXO database of the transaction pool.
     */
    public TransactionProcessor(@NotNull TransactionValidator transactionValidator,
                                @NotNull TransactionPool transactionPool,
                                @NotNull ChainUTXODatabase utxoFromChain,
                                @NotNull UTXODatabase utxoFromPool) {
        LOGGER.fine("Initializing new transaction processor.");
        this.transactionValidator = transactionValidator;
        this.transactionPool = transactionPool;
        this.utxoFromChain = utxoFromChain;
        this.utxoFromPool = utxoFromPool;
    }

    /**
     * Add a new transaction to the transaction pool.
     *
     * @param transaction
     *     The new transaction.
     * @return The status of the transaction in the transaction pool and any orphans that are
     * added as a result.
     * @throws DatabaseException
     *     When either of the UTXO databases is not available.
     */
    public ProcessedTransactionResult processNewTransaction(@NotNull Transaction transaction) throws DatabaseException {
        LOGGER.fine("Processing new transaction.");
        ProcessedTransactionStatus status = processSingleTransaction(transaction);

        // Check if any orphan transactions can be added as well
        List<Transaction> removed = removeValidOrphans(transaction.computeHash());
        LOGGER.info(() -> MessageFormat.format("{0} orphans can be added as well (as dependent).", removed.size()));

        // Set removed orphan as dependent and set outputs as unspent
        for (Transaction orphan : removed) {
            ProcessedTransactionStatus orphanStatus = processSingleTransaction(orphan);
            assert orphanStatus == ProcessedTransactionStatus.DEPENDENT;
            // TODO: what if assert fails?
        }

        return new ProcessedTransactionResult(status, removed);
    }

    @NotNull
    private List<Transaction> removeValidOrphans(@NotNull Hash hash) {
        return transactionPool.removeValidOrphansFromParent(hash, t -> {
            try {
                return checkInputs(t) != ProcessedTransactionStatus.ORPHAN;
            }
            catch (DatabaseException e) {
                return false;
            }
        });
    }

    private ProcessedTransactionStatus processSingleTransaction(@NotNull Transaction transaction) throws DatabaseException {
        if (!transactionValidator.checkTransactionValid(transaction)) {
            LOGGER.info("New transaction is invalid.");
            return ProcessedTransactionStatus.INVALID;
        }

        Hash hash = transaction.computeHash();

        // Check if already stored
        if (transactionPool.isHashKnown(hash)) {
            LOGGER.info("New transaction is already present in transaction pool.");
            return ProcessedTransactionStatus.ALREADY_STORED;
        }

        // Check the inputs
        ProcessedTransactionStatus status = checkInputs(transaction);

        // If orphan, add to orphan set and do nothing
        if (status == ProcessedTransactionStatus.ORPHAN) {
            transactionPool.addOrphanTransaction(transaction);
            LOGGER.info("New transaction is added as orphan.");
            return ProcessedTransactionStatus.ORPHAN;
        }

        // Validated transaction: update pool UTXO set
        LOGGER.fine("New transaction can be added to transaction pool. Set outputs unspent in transaction pool UTXO.");
        utxoFromPool.setOutputsUnspent(transaction, Magic.TRANSACTION_POOL_HEIGHT);

        if (status == ProcessedTransactionStatus.DEPENDENT) {
            LOGGER.info("New transaction is added as dependent.");
            transactionPool.addDependentTransaction(transaction);
        } else {
            LOGGER.info("New transaction is added as independent.");
            transactionPool.addIndependentTransaction(transaction);
        }

        return status;
    }

    private ProcessedTransactionStatus checkInputs(@NotNull Transaction transaction) throws DatabaseException {
        LOGGER.finest("Checking inputs of transaction.");
        boolean isDependent = false;
        for (Input input : transaction.getInputs()) {
            if (utxoFromPool.isUnspent(input)) {
                // Input references UTXO from pool, transaction is at least dependent
                isDependent = true;
            } else if (!utxoFromChain.isUnspent(input)) {
                // Input is in neither UTXO, transaction is orphan
                LOGGER.finest("Transaction is orphan.");
                return ProcessedTransactionStatus.ORPHAN;
            }
        }

        if (isDependent) {
            LOGGER.finest("Transaction is dependent.");
            return ProcessedTransactionStatus.DEPENDENT;
        }

        LOGGER.finest("Transaction is independent.");
        return ProcessedTransactionStatus.INDEPENDENT;
    }
}
