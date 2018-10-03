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
    private final Hash referencedTransaction;

    /**
     * Referenced output index.
     */
    @ProtoField
    private final int referencedOutputIndex;

    /**
     * Create a new Input
     * @param signature digital signature of this input
     * @param referencedTransaction transaction referenced by this input
     * @param referencedOutputIndex
     */
    public Input(Signature signature, Hash referencedTransaction, int referencedOutputIndex) {
        this.signature = signature;
        this.referencedTransaction = referencedTransaction;
        this.referencedOutputIndex = referencedOutputIndex;
    }

    public Signature getSignature() {
        return signature;
    }

    public Hash getReferencedTransaction() {
        return referencedTransaction;
    }

    public int getReferencedOutputIndex() {
        return referencedOutputIndex;
    }

    @ProtoClass(BrabocoinProtos.Input.class)
    public static class Builder {
        @ProtoField
        private Signature signature;
        @ProtoField
        private Hash.Builder referencedTransaction;
        @ProtoField
        private int referencedOutputIndex;

        public Builder setSignature(Signature signature) {
            this.signature = signature;
            return this;
        }

        public Builder setReferencedTransaction(Hash.Builder referencedTransaction) {
            this.referencedTransaction = referencedTransaction;
            return this;
        }

        public void setReferencedOutputIndex(int referencedOutputIndex) {
            this.referencedOutputIndex = referencedOutputIndex;
        }

        public Input createInput() {
            return new Input(signature, referencedTransaction.createHash(), referencedOutputIndex);
        }
    }
}
