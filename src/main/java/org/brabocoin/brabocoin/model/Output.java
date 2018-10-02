package org.brabocoin.brabocoin.model;

/**
 * Implementation of an output of a transaction
 */
public class Output {
    /**
     * Address of the receiver of the output amount.
     */
    private final Hash address;

    /**
     * Amount paid to the output receiver.
     */
    private final long amount;

    /**
     * The index number of the output,
     * identifying which output of the transaction is referenced.
     */
    private int outputIndex;

    /**
     * Create a new output
     * @param address address of the receiver of the output amount
     * @param amount amount paid to the output receiver
     * @param outputIndex index number of the output in the overlaying transaction
     */
    public Output(Hash address, long amount, int outputIndex) {
        this.address = address;
        this.amount = amount;
        this.outputIndex = outputIndex;
    }

    public Hash getAddress() {
        return address;
    }

    public long getAmount() {
        return amount;
    }

    public int getOutputIndex() {
        return outputIndex;
    }
}
