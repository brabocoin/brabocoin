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
     * Create a new Input
     * @param signature digital signature of this input
     * @param referencedTransaction transaction referenced by this input
     */
    public Input(Signature signature, Transaction referencedTransaction) {
        this.signature = signature;
        this.referencedTransaction = referencedTransaction;
    }

    public Signature getSignature() {
        return signature;
    }

    public Transaction getReferencedTransaction() {
        return referencedTransaction;
    }

    @ProtoClass(BrabocoinProtos.Input.class)
    public static class Builder {
        @ProtoField
        private Signature signature;
        @ProtoField
        private Transaction.Builder referencedTransaction;

        public Builder setSignature(Signature signature) {
            this.signature = signature;
            return this;
        }

        public Builder setReferencedTransaction(Transaction.Builder referencedTransaction) {
            this.referencedTransaction = referencedTransaction;
            return this;
        }

        public Input createInput() {
            return new Input(signature, referencedTransaction.createTransaction());
        }
    }
}
