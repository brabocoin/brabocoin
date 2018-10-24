package org.brabocoin.brabocoin.processor;

import org.brabocoin.brabocoin.dal.HashMapDB;
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
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.validation.Consensus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test {@link org.brabocoin.brabocoin.processor.UTXOProcessor}.
 */
class UTXOProcessorTest {

    private UTXODatabase database;
    private Consensus consensus;
    private UTXOProcessor processor;

    @BeforeEach
    void setUp() throws DatabaseException {
        consensus = new Consensus();
        database = new UTXODatabase(new HashMapDB(), consensus);
        processor = new UTXOProcessor(database);
    }

    @Test
    void processCoinbaseBlockConnected() throws DatabaseException {
        // TODO: change to coinbase?
        Output output = Simulation.randomOutput();
        Transaction transaction = new Transaction(
            new ArrayList<>(),
            Collections.singletonList(output)
        );

        Block block = new Block(
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomHash(),
            Simulation.randomByteString(),
            0,
            0,
            Collections.singletonList(transaction)
        );

        // Check undo is empty
        BlockUndo undo = processor.processBlockConnected(block);
        assertEquals(1, undo.getTransactionUndos().size());
        TransactionUndo transactionUndo = undo.getTransactionUndos().get(0);
        assertTrue(transactionUndo.getOutputInfoList().isEmpty());

        // Check UTXO is added
        assertTrue(database.isUnspent(transaction.computeHash(), 0));
    }

    @Test
    void processRegularBlock() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1).get(0);
        Hash address = Simulation.randomHash();

        // Set all inputs of new block manually unspent
        for (Transaction transaction : block.getTransactions()) {
            for (Input input : transaction.getInputs()) {
                database.addUnspentOutputInfo(
                    input.getReferencedTransaction(),
                    input.getReferencedOutputIndex(),
                    new UnspentOutputInfo(false, 42, 67, address)
                );
            }
        }

        // Process block
        BlockUndo blockUndo = processor.processBlockConnected(block);

        // Check undo
        assertEquals(block.getTransactions().size(), blockUndo.getTransactionUndos().size());

        List<Transaction> transactions = block.getTransactions();
        for (int i = 0; i < transactions.size(); i++) {
            Transaction transaction = transactions.get(i);
            Hash hash = transaction.computeHash();
            TransactionUndo undo = blockUndo.getTransactionUndos().get(i);

            assertEquals(transaction.getInputs().size(), undo.getOutputInfoList().size());

            // Check if all inputs are spent / outputs are unspent
            for (Input input : transaction.getInputs()) {
                assertFalse(database.isUnspent(input));
            }

            for (int outputIndex = 0; outputIndex < transaction.getOutputs().size(); outputIndex++) {
                assertTrue(database.isUnspent(hash, outputIndex));
            }
        }

        assertEquals(block.computeHash(), database.getLastProcessedBlockHash());
    }

    @Test
    void processBlockDisconnected() throws DatabaseException {
        Block block = Simulation.randomBlockChainGenerator(1, consensus.getGenesisBlock().computeHash(), 1).get(0);
        Hash address = Simulation.randomHash();

        // Set all inputs of new block manually unspent
        for (Transaction transaction : block.getTransactions()) {
            for (Input input : transaction.getInputs()) {
                database.addUnspentOutputInfo(
                    input.getReferencedTransaction(),
                    input.getReferencedOutputIndex(),
                    new UnspentOutputInfo(false, 42, 67, address)
                );
            }
        }

        // Process block
        BlockUndo blockUndo = processor.processBlockConnected(block);

        // Disconnect block
        processor.processBlockDisconnected(block, blockUndo);

        // Check if all inputs are unspent / outputs are spent
        for (Transaction transaction : block.getTransactions()) {
            Hash hash = transaction.computeHash();

            for (Input input : transaction.getInputs()) {
                assertTrue(database.isUnspent(input));
            }

            for (int i = 0; i < transaction.getOutputs().size(); i++) {
                assertFalse(database.isUnspent(hash, i));
            }
        }

        assertEquals(consensus.getGenesisBlock().computeHash(), database.getLastProcessedBlockHash());
    }
}
