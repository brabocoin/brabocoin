package org.brabocoin.brabocoin.data;

/**
 * Implementation of an output of a transaction
 */
public class OutputImpl implements Output {
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
    public OutputImpl(Hash address, long amount, int outputIndex) {
        this.address = address;
        this.amount = amount;
        this.outputIndex = outputIndex;
    }

    @Override
    public Hash getAddress() {
        return address;
    }

    @Override
    public long getAmount() {
        return amount;
    }

    @Override
    public int getOutputIndex() {
        return outputIndex;
    }
}
