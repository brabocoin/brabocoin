package org.brabocoin.brabocoin.data;

/**
 * An output of a transaction.
 */
public interface Output {
    /**
     * Get the address of the receiver.
     * @return The address of the receiver
     */
    Hash getAddress();

    /**
     * Get the amount paid to the receiver.
     * @return The amount
     */
    long getAmount();

    /**
     * Get the index number of the output,
     * identifying which output of the transaction is referenced.
     * @return The index number
     */
    int getOutputIndex();

}
