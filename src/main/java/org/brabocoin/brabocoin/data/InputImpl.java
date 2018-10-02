package org.brabocoin.brabocoin.data;

/**
 * Implementation of an input of a transaction
 */
public class InputImpl implements Input {
    /**
     * Digital signature belonging to this input.
     */
    private final Signature signature;

    /**
     * Output referenced by this input.
     */
    private final Output referencedOutput;

    /**
     * Create a new Input
     * @param signature digital signature of this input
     * @param referencedOutput output referenced by this input
     */
    public InputImpl(Signature signature, Output referencedOutput) {
        this.signature = signature;
        this.referencedOutput = referencedOutput;
    }

    @Override
    public Signature getSignature() {
        return signature;
    }

    @Override
    public Output getReferencedOutput() {
        return referencedOutput;
    }
}
