package org.brabocoin.brabocoin.utxo;

import org.brabocoin.brabocoin.dal.utxo.UTXODatabase;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Maintains an index of the unspent transaction outputs (UTXO) set.
 */
public class UTXOSet {

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
     * Process the inputs and outputs of all transactions in a newly connected block.
     * <p>
     * The referenced outputs of the inputs of the new transactions are marked as spent, and the
     * outputs of the new transactions are marked as unspent.
     *
     * @param block
     *     The newly connected block.
     * @throws DatabaseException
     *     When the database backend is not available.
     */
    public void processBlockConnected(@NotNull Block block) throws DatabaseException {
        // TODO: validate everything!
        for (Transaction transaction : block.getTransactions()) {
            // Set all inputs as spent
            for (Input input : transaction.getInputs()) {
                database.setOutputSpent(input.getReferencedTransaction(),
                    input.getReferencedOutputIndex()
                );
            }

            // Set all outputs as unspent
            database.setOutputsUnspent(transaction, block.getBlockHeight());
        }
    }

    /**
     * Process the inputs and outputs of all transactions in a disconnected block.
     * <p>
     * All outputs of the transactions in the block are marked as spent, and the inputs are
     * marked as unspent by applying the block revert file.
     *
     * @param block
     *     The disconnected block.
     * @throws DatabaseException
     *     When the database backend is not available.
     */
    public void processBlockDisconnected(@NotNull Block block) throws DatabaseException {
        List<Transaction> transactions = block.getTransactions();

        // TODO: validate everything!
        // Undo transactions in reverse order
        for (int i = transactions.size() - 1; i >= 0; i--) {
            Transaction transaction = transactions.get(i);
            Hash hash = transaction.computeHash();

            // Set all outputs as spent
            List<Output> outputs = transaction.getOutputs();
            for (int outputIndex = 0; outputIndex < outputs.size(); outputIndex++) {
                database.setOutputSpent(hash, outputIndex);
            }

            // TODO: set all inputs as unspent (need revert files because block height of the ...
            // TODO: ... referenced transaction needs to be known)
        }
    }
}
