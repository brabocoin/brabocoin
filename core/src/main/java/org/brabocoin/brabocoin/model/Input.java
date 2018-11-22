package org.brabocoin.brabocoin.model;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.model.crypto.Signature;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Input of a transaction.
 * <p>
 * An input of an transaction references an (up to now unspent) output of an earlier transaction,
 * and contains a signature to verify the input and all outputs of the transaction.
 */
@ProtoClass(BrabocoinProtos.Input.class)
public class Input implements ProtoModel<Input> {

    /**
     * Digital signature that validates this input and all outputs in the transaction.
     */
    @ProtoField
    private final @Nullable Signature signature;

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
    public Input(@Nullable Signature signature, @NotNull Hash referencedTransaction,
                 int referencedOutputIndex) {
        this.signature = signature;
        this.referencedTransaction = referencedTransaction;
        this.referencedOutputIndex = referencedOutputIndex;
    }

    public @Nullable Signature getSignature() {
        return signature;
    }

    public @NotNull Hash getReferencedTransaction() {
        return referencedTransaction;
    }

    public int getReferencedOutputIndex() {
        return referencedOutputIndex;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    @ProtoClass(BrabocoinProtos.Input.class)
    public static class Builder implements ProtoBuilder<Input> {

        @ProtoField
        private Signature.Builder signature;
        @ProtoField
        private Hash.Builder referencedTransaction;
        @ProtoField
        private int referencedOutputIndex;

        public Builder setSignature(Signature.Builder signature) {
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

        @Override
        public Input build() {
            return new Input(signature.build(), referencedTransaction.build(), referencedOutputIndex);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Input input = (Input) o;
        return referencedOutputIndex == input.referencedOutputIndex &&
                referencedTransaction.equals(input.referencedTransaction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referencedTransaction, referencedOutputIndex);
    }
}
