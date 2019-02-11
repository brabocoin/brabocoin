package org.brabocoin.brabocoin.processor;

import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
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
 * Processes mutations to the UTXO set.
 */
public class UTXOProcessor {

    private static final Logger LOGGER = Logger.getLogger(UTXOProcessor.class.getName());

    /**
     * UTXO database.
     */
    private final @NotNull ChainUTXODatabase database;

    /**
     * Creates a new UTXO processor for the given UTXO database.
     *
     * @param database
     *     THhe UTXO database.
     */
    public UTXOProcessor(@NotNull ChainUTXODatabase database) {
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
            // Set all outputs as unspent
            database.setOutputsUnspent(transaction, block.getBlockHeight());

            if (transaction.isCoinbase()) {
                continue;
            }

            List<UnspentOutputInfo> outputInfos = new ArrayList<>();

            // Set all inputs as spent
            for (Input input : transaction.getInputs()) {
                outputInfos.add(database.findUnspentOutputInfo(input));

                database.setOutputSpent(
                    input.getReferencedTransaction(),
                    input.getReferencedOutputIndex()
                );
            }

            // Add to transaction undo
            undos.add(new TransactionUndo(outputInfos));
        }

        // Move block pointer
        database.setLastProcessedBlockHash(block.getHash());

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
    public void processBlockDisconnected(@NotNull Block block,
                                         @NotNull BlockUndo blockUndo) throws DatabaseException {
        LOGGER.finest("Process disconnected block in UTXO set.");

        List<Transaction> transactions = block.getTransactions();
        List<TransactionUndo> undos = blockUndo.getTransactionUndos();

        // TODO: validate everything!
        // Undo transactions in reverse order
        for (int i = transactions.size() - 1; i >= 0; i--) {
            Transaction transaction = transactions.get(i);

            Hash hash = transaction.getHash();
            // Set all outputs as spent
            List<Output> outputs = transaction.getOutputs();
            for (int outputIndex = 0; outputIndex < outputs.size(); outputIndex++) {
                database.setOutputSpent(hash, outputIndex);
            }

            if (transaction.isCoinbase()) {
                continue;
            }

            // Unspent output info is not present for the coinbase transaction
            // The first tx is the coinbase tx. Hence, we can skip using -1.
            List<UnspentOutputInfo> inputInfos = undos.get(i - 1).getOutputInfoList();
            // Apply undo for the inputs
            List<Input> inputs = transaction.getInputs();
            for (int inputIndex = 0; inputIndex < inputs.size(); inputIndex++) {
                Input input = inputs.get(inputIndex);
                UnspentOutputInfo info = inputInfos.get(inputIndex);

                database.addUnspentOutputInfo(
                    input.getReferencedTransaction(),
                    input.getReferencedOutputIndex(),
                    info
                );
            }
        }

        // Move block pointer
        database.setLastProcessedBlockHash(block.getPreviousBlockHash());
    }

    public @NotNull Hash getLastProcessedBlockHash() throws DatabaseException {
        return database.getLastProcessedBlockHash();
    }
}
