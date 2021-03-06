package org.brabocoin.brabocoin.processor;

import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.dal.TransactionPoolListener;
import org.brabocoin.brabocoin.dal.UTXODatabase;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.ValidationStatus;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidationResult;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.brabocoin.brabocoin.util.ByteUtil.toHexString;

/**
 * Processes transactions to and from the {@link TransactionPool}.
 */
public class TransactionProcessor implements TransactionPoolListener {

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
     * @param utxoFromPool
     *     The UTXO database of the transaction pool.
     */
    public TransactionProcessor(@NotNull TransactionValidator transactionValidator,
                                @NotNull TransactionPool transactionPool,
                                @NotNull UTXODatabase utxoFromPool) {
        LOGGER.fine("Initializing new transaction processor.");
        this.transactionValidator = transactionValidator;
        this.transactionPool = transactionPool;
        this.utxoFromPool = utxoFromPool;

        this.transactionPool.addListener(this);
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
    public ProcessedTransactionResult processNewTransaction(
        @NotNull Transaction transaction) throws DatabaseException {
        LOGGER.fine("Processing new transaction.");
        ValidationStatus status = processTransaction(transaction);
        List<Transaction> orphans = addValidOrphans(transaction.getHash());

        transactionPool.limitTransactionPoolSize();

        return new ProcessedTransactionResult(status, orphans);
    }

    private @NotNull List<Transaction> addValidOrphans(@NotNull Hash hash) {
        // Check if any orphan transactions can be added as well
        List<Transaction> removed = removeValidOrphans(hash);
        LOGGER.info(() -> MessageFormat.format(
            "{0} orphans are added to the transaction pool.",
            removed.size()
        ));

        return removed;
    }

    private @NotNull List<Transaction> removeValidOrphans(@NotNull Hash hash) {
        return transactionPool.removeValidOrphansFromParent(hash, t -> {
            try {
                ValidationStatus status = transactionValidator.validate(t, TransactionValidator.ORPHAN, true)
                    .getStatus();

                if (status != ValidationStatus.ORPHAN) {
                    return processDeorphanatedTransaction(t) == ValidationStatus.VALID;
                }
            }
            catch (DatabaseException ignored) {
            }

            return false;
        });
    }

    private ValidationStatus processTransaction(
        @NotNull Transaction transaction) throws DatabaseException {
        TransactionValidationResult result =
            transactionValidator.validate(transaction, TransactionValidator.ALL, true);

        processTransactionWithoutValidation(transaction, result);

        return result.getStatus();
    }

    private ValidationStatus processDeorphanatedTransaction(
        @NotNull Transaction transaction) throws DatabaseException {
        TransactionValidationResult result = transactionValidator.validate(
            transaction, TransactionValidator.AFTER_ORPHAN, true);

        processTransactionWithoutValidation(transaction, result);

        return result.getStatus();
    }

    private void processTransactionWithoutValidation(@NotNull Transaction transaction,
                                                     TransactionValidationResult result) throws DatabaseException {

        ValidationStatus status = result.getStatus();
        if (status == ValidationStatus.INVALID) {
            LOGGER.log(Level.INFO, MessageFormat.format("New transaction is invalid, rulebook result: {0}", result));
            transactionPool.addRejected(transaction, result);
            return;
        }

        // If orphan, add to orphan set and do nothing
        if (status == ValidationStatus.ORPHAN) {
            // Check if already stored as orphan
            if (!transactionPool.isOrphan(transaction.getHash())) {
                transactionPool.addOrphanTransaction(transaction);
                LOGGER.info("New transaction is added as orphan.");
            }
            return;
        }

        if (transactionValidator.validate(transaction, TransactionValidator.ORPHAN, false).isPassed()) {
            LOGGER.info("New transaction is added as independent.");
            transactionPool.addIndependentTransaction(transaction);
        }
        else {
            LOGGER.info("New transaction is added as dependent.");
            transactionPool.addDependentTransaction(transaction);
        }
    }

    /**
     * Update the transaction pool on the event a new block is connected to the main chain.
     * <p>
     * All transactions in the connected block are now in the blockchain and are removed from the
     * transaction pool. By this change, dependent transactions might be promoted to independent
     * transactions.
     *
     * @param block
     *     The block that is connected to the main chain.
     * @throws DatabaseException
     *     When the UTXO database is not available.
     */
    public void processTopBlockConnected(@NotNull Block block) throws DatabaseException {
        LOGGER.fine("Processing top block connected.");

        for (Transaction transaction : block.getTransactions()) {
            if (transaction.isCoinbase()) {
                continue;
            }

            Hash hash = transaction.getHash();

            // Remove from pool
            LOGGER.finest(() -> MessageFormat.format(
                "Removing transaction {0} from transaction pool.",
                toHexString(hash.getValue())
            ));
            transactionPool.removeValidatedTransaction(hash);

            // Remove from orphans
            transactionPool.removeOrphan(hash);
        }

        // Promote dependent transactions to independent when possible
        // Do this in a separate loop to ensure UTXO from pool is in sync with the UTXO from chain
        // Otherwise independent transactions might not be recognized at first
        LOGGER.fine("Promoting dependent transactions to dependent when possible.");
        for (Transaction transaction : block.getTransactions()) {
            Hash hash = transaction.getHash();
            transactionPool.promoteDependentToIndependentFromParent(
                hash,
                t -> transactionValidator.validate(t, TransactionValidator.ORPHAN, false).isPassed()
            );
        }

        transactionPool.limitTransactionPoolSize();
    }

    /**
     * Update the transaction pool on the event the top block is disconnected from the main chain.
     * <p>
     * All transactions in the disconnected block are added back to the transaction pool. By this
     * change, some independent transactions may become dependent, and some orphan transactions
     * (that were double-spending) may become valid.
     * <p>
     * Note that all transactions in the block are already validated.
     *
     * @param block
     *     The block that is disconnected from the main chain.
     * @throws DatabaseException
     *     When the UTXO database is not available.
     */
    public void processTopBlockDisconnected(@NotNull Block block) throws DatabaseException {
        LOGGER.fine("Processing top block disconnected.");

        for (Transaction transaction : block.getTransactions()) {
            if (transaction.isCoinbase()) {
                // Do not add the coinbase transaction to the transaction pool.
                // Instead, make sure depending transactions on this coinbase are demoted to orphan
                transactionPool.demoteToOrphan(transaction.getHash());
            }
            else {
                // Add transactions to pool and update pool UTXO set
                processTransactionWithoutValidation(transaction, TransactionValidationResult.passed());
                LOGGER.finest(() -> MessageFormat.format(
                    "Added transaction {0} to transaction pool.",
                    toHexString(transaction.getHash().getValue())
                ));

                // Some previously independent transactions may depend the processed transaction
                // making it dependent
                transactionPool.demoteIndependentToDependent(transaction.getHash());

                // Some orphan transactions could be double-spending outputs that are used by the
                // transactions in the disconnected block. These orphan transactions now become
                // valid.
                for (Input input : transaction.getInputs()) {
                    addValidOrphans(input.getReferencedTransaction());
                }
            }
        }

        transactionPool.limitTransactionPoolSize();
    }

    @Override
    public void onTransactionAddedToPool(@NotNull Transaction transaction) {
        try {
            utxoFromPool.setOutputsUnspent(transaction, Constants.TRANSACTION_POOL_HEIGHT);
        }
        catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Transacton pool UTXO could not be updated.", e);
            throw new RuntimeException("Transaction pool UTXO could not be updated.", e);
        }
    }

    @Override
    public void onTransactionRemovedFromPool(@NotNull Transaction transaction) {
        try {
            for (int i = 0; i < transaction.getOutputs().size(); i++) {
                utxoFromPool.setOutputSpent(transaction.getHash(), i);
            }
        }
        catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Transacton pool UTXO could not be updated.", e);
            throw new RuntimeException("Transaction pool UTXO could not be updated.", e);
        }
    }
}
