package org.brabocoin.brabocoin.model;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.jetbrains.annotations.NotNull;

/**
 * Input of a transaction.
 * <p>
 * An input of an transaction references an (up to now unspent) output of an earlier transaction,
 * and contains a signature to verify the input and all outputs of the transaction.
 */
@ProtoClass(BrabocoinProtos.Input.class)
public class Input {

    /**
     * Digital signature that validates this input and all outputs in the transaction.
     */
    @ProtoField
    private final @NotNull Signature signature;

    /**
     * Transaction in which the (up to now) unspent output is contained.
     */
    @ProtoField
    private final @NotNull Hash referencedTransaction;

    /**
     * Referenced output index.
     */
    @ProtoField
    private final int referencedOutputIndex;

    /**
     * Create a new transaction input.
     *
     * @param signature
     *         Digital signature of this input.
     * @param referencedTransaction
     *         Transaction of the referenced output.
     * @param referencedOutputIndex
     *         Index of the referenced output.
     */
    public Input(@NotNull Signature signature, @NotNull Hash referencedTransaction,
                 int referencedOutputIndex) {
        this.signature = signature;
        this.referencedTransaction = referencedTransaction;
        this.referencedOutputIndex = referencedOutputIndex;
    }

    public @NotNull Signature getSignature() {
        return signature;
    }

    public @NotNull Hash getReferencedTransaction() {
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

        /**
         * Creates the transaction input.
         *
         * @return The transaction input.
         */
        public Input createInput() {
            return new Input(signature, referencedTransaction.createHash(), referencedOutputIndex);
        }
    }
}
