package org.brabocoin.brabocoin.model;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.model.crypto.Signature;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Input of a transaction.
 * <p>
 * An input of an transaction references an (up to now unspent) output of an earlier transaction.
 */
@ProtoClass(BrabocoinProtos.Input.class)
public class Input implements ProtoModel<Input> {

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
     * @param referencedTransaction
     *     Transaction of the referenced output.
     * @param referencedOutputIndex
     *     Index of the referenced output.
     */
    public Input(@NotNull Hash referencedTransaction,
                 int referencedOutputIndex) {
        this.referencedTransaction = referencedTransaction;
        this.referencedOutputIndex = referencedOutputIndex;
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
        private Hash.Builder referencedTransaction;
        @ProtoField
        private int referencedOutputIndex;

        public Builder setReferencedTransaction(Hash.Builder referencedTransaction) {
            this.referencedTransaction = referencedTransaction;
            return this;
        }

        public void setReferencedOutputIndex(int referencedOutputIndex) {
            this.referencedOutputIndex = referencedOutputIndex;
        }

        @Override
        public Input build() {
            return new Input(referencedTransaction.build(), referencedOutputIndex);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Input input = (Input)o;
        return referencedOutputIndex == input.referencedOutputIndex &&
            referencedTransaction.equals(input.referencedTransaction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referencedTransaction, referencedOutputIndex);
    }
}
