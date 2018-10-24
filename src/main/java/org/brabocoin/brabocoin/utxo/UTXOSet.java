package org.brabocoin.brabocoin.utxo;

import org.brabocoin.brabocoin.dal.UTXODatabase;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.dal.BlockUndo;
import org.brabocoin.brabocoin.model.dal.TransactionUndo;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Maintains an index of the unspent transaction outputs (UTXO) set.
 */
public class UTXOSet {

    private static final Logger LOGGER = Logger.getLogger(UTXOSet.class.getName());

    /**
     * The UTXO database.
     */
    private final @NotNull UTXODatabase database;

    /**
     * Creates a new UTXO set manager.
     *
     * @param database
     *     The UTXO database backend.
     */
    public UTXOSet(@NotNull UTXODatabase database) {
        this.database = database;
    }

    /**
     * Check whether the referenced output of this input is unspent.
     * <p>
     * If the referenced output is not present in the UTXO set, it is considered to be spent.
     *
     * @param input
     *     The input.
     * @return Whether the referenced output of the input is unspent.
     * @throws DatabaseException
     *     When the UTXO database is not available.
     */
    public boolean isUnspent(@NotNull Input input) throws DatabaseException {
        return database.isUnspent(input);
    }

    /**
     * Check whether the output specified by the index and hash of the transaction is unspent.
     * <p>
     * If the referenced output is not present in the UTXO set, it is considered to be spent.
     *
     * @param transactionHash
     *     The hash of the transaction of the specified output.
     * @param outputIndex
     *     The index of the output in the transaction.
     * @return Whether the output is unspent.
     * @throws DatabaseException
     *     When the UTXO database is not available.
     */
    public boolean isUnspent(@NotNull Hash transactionHash, int outputIndex) throws DatabaseException {
        return database.isUnspent(transactionHash, outputIndex);
    }

    /**
     * Retrieves the hash of the block in the main chain up to which the UTXO set is up to date.
     *
     * @return The hash of the block up to which the UTXO set is updated.
     * @throws DatabaseException
     *     When the UTXO database is not available.
     */
    public @NotNull Hash getLastProcessedBlockHash() throws DatabaseException {
        return database.getLastProcessedBlockHash();
    }

    /**
     * Process the inputs and outputs of all transactions in a newly connected block.
     * <p>
     * The referenced outputs of the inputs of the new transactions are marked as spent, and the
     * outputs of the new transactions are marked as unspent.
     *
     * @param block
     *     The newly connected block.
     * @return The block undo data that can be used to restore the UTXO set when this block is
     * disconnected.
     * @throws DatabaseException
     *     When the database backend is not available.
     */
    public @NotNull BlockUndo processBlockConnected(@NotNull Block block) throws DatabaseException {
        LOGGER.finest("Process connected block in UTXO set.");

        // TODO: validate everything!

        List<TransactionUndo> undos = new ArrayList<>();

        for (Transaction transaction : block.getTransactions()) {
            List<UnspentOutputInfo> outputInfos = new ArrayList<>();

            // Set all inputs as spent
            for (Input input : transaction.getInputs()) {
                outputInfos.add(database.findUnspentOutputInfo(input));

                database.setOutputSpent(input.getReferencedTransaction(),
                    input.getReferencedOutputIndex()
                );
            }

            // Add to transaction undo
            undos.add(new TransactionUndo(outputInfos));

            // Set all outputs as unspent
            database.setOutputsUnspent(transaction, block.getBlockHeight());
        }

        // Move block pointer
        database.setLastProcessedBlockHash(block.computeHash());

        return new BlockUndo(undos);
    }

    /**
     * Process the inputs and outputs of all transactions in a disconnected block.
     * <p>
     * All outputs of the transactions in the block are marked as spent, and the inputs are
     * marked as unspent by applying the block undo file.
     *
     * @param block
     *     The disconnected block.
     * @param blockUndo
     *     The undo data for the disconnected block.
     * @throws DatabaseException
     *     When the database backend is not available.
     */
    public void processBlockDisconnected(@NotNull Block block, @NotNull BlockUndo blockUndo) throws DatabaseException {
        LOGGER.finest("Process disconnected block in UTXO set.");

        List<Transaction> transactions = block.getTransactions();
        List<TransactionUndo> undos = blockUndo.getTransactionUndos();

        // TODO: validate everything!
        // Undo transactions in reverse order
        for (int i = transactions.size() - 1; i >= 0; i--) {
            Transaction transaction = transactions.get(i);
            List<UnspentOutputInfo> inputInfos = undos.get(i).getOutputInfoList();
            Hash hash = transaction.computeHash();

            // Set all outputs as spent
            List<Output> outputs = transaction.getOutputs();
            for (int outputIndex = 0; outputIndex < outputs.size(); outputIndex++) {
                database.setOutputSpent(hash, outputIndex);
            }

            // Apply undo for the inputs
            List<Input> inputs = transaction.getInputs();
            for (int inputIndex = 0; inputIndex < inputs.size(); inputIndex++) {
                Input input = inputs.get(inputIndex);
                UnspentOutputInfo info = inputInfos.get(inputIndex);

                database.addUnspentOutputInfo(input.getReferencedTransaction(),
                    input.getReferencedOutputIndex(),
                    info
                );
            }
        }

        // Move block pointer
        database.setLastProcessedBlockHash(block.getPreviousBlockHash());
    }
}
