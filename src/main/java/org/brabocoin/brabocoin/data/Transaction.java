package org.brabocoin.brabocoin.data;

import java.util.List;

/**
 * A general transaction from several inputs to several outputs.
 */
public interface Transaction {
    /**
     * Get the list of inputs of the transaction.
     * @return The inputs
     */
    List<Input> getInputs();

    /**
     * Get the list of outputs of the transaction.
     * @return The outputs
     */
    List<Output> getOutputs();

    /**
     * Get the hash of the transaction.
     * @return The transaction hash
     */
    Hash getTransactionId();
}
