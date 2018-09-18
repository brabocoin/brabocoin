package org.brabocoin.brabocoin.data;

/**
 * An input of a transaction.
 */
public interface Input {
    /**
     * Get the signature of the input.
     * @return The signature
     */
    Signature getSignature();

    /**
     * Get the output referenced by this input.
     * @return The referenced output
     */
    Output getReferencedOutput();

}
