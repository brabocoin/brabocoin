package org.brabocoin.brabocoin.data;

import java.util.List;

/**
 * Implementation of transaction.
 */
public class TransactionImpl implements Transaction {

    /**
     * Inputs used by the transaction.
     */
    private final List<Input> inputs;

    /**
     * Outputs used by the transaction.
     */
    private final List<Output> outputs;

    /**
     * Hash of the transaction.
     */
    private final Hash transactionId;

    /**
     * Create a new transaction.
     * @param inputs inputs used by the transaction.
     * @param outputs outputs used by the transaction.
     * @param transactionId hash of the transaction.
     */
    public TransactionImpl(List<Input> inputs, List<Output> outputs,
            Hash transactionId) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.transactionId = transactionId;
    }

    @Override
    public List<Input> getInputs() {
        return inputs;
    }

    @Override
    public List<Output> getOutputs() {
        return outputs;
    }

    @Override
    public Hash getTransactionId() {
        return transactionId;
    }
}
