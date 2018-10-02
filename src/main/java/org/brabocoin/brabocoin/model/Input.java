package org.brabocoin.brabocoin.model;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;

/**
 * Implementation of an input of a transaction
 */
@ProtoClass(BrabocoinProtos.Input.class)
public class Input {
    /**
     * Digital signature belonging to this input.
     */
    @ProtoField
    private final Signature signature;

    /**
     * Transaction referenced by this input.
     */
    @ProtoField
    private final Transaction referencedTransaction;

    /**
     * Ouput index for the referenced transaction.
     */
    @ProtoField
    private final int outputIndex;

    /**
     * Create a new Input
     * @param signature digital signature of this input
     * @param referencedTransaction transaction referenced by this input
     * @param outputIndex output index for the referenced transaction
     */
    public Input(Signature signature, Transaction referencedTransaction, int outputIndex) {
        this.signature = signature;
        this.referencedTransaction = referencedTransaction;
        this.outputIndex = outputIndex;
    }

    public Signature getSignature() {
        return signature;
    }

    public Transaction getReferencedTransaction() {
        return referencedTransaction;
    }

    public int getOutputIndex() {
        return outputIndex;
    }
}
