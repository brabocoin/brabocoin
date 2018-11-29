package org.brabocoin.brabocoin.model;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.crypto.Hashing;
import org.brabocoin.brabocoin.model.crypto.Signature;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transaction with brabocoin inputs and outputs.
 */
@ProtoClass(BrabocoinProtos.UnsignedTransaction.class)
public class UnsignedTransaction implements ProtoModel<UnsignedTransaction> {

    /**
     * Inputs used by the transaction.
     */
    @ProtoField
    protected final @NotNull List<Input> inputs;

    /**
     * Outputs used by the transaction.
     */
    @ProtoField
    protected final @NotNull List<Output> outputs;

    /**
     * Cached hash of this transaction.
     */
    private Hash hash;

    /**
     * Create a new unsigned transaction.
     *
     * @param inputs
     *     Inputs used by the transaction.
     * @param outputs
     *     Outputs used by the transaction.
     */
    public UnsignedTransaction(@NotNull List<Input> inputs, @NotNull List<Output> outputs) {
        this.inputs = new ArrayList<>(inputs);
        this.outputs = new ArrayList<>(outputs);
    }

    /**
     * Copy constructor.
     *
     * @param unsignedTransaction
     *     Unsigned Transaction to copy
     */
    private UnsignedTransaction(UnsignedTransaction unsignedTransaction) {
        this(unsignedTransaction.inputs, unsignedTransaction.outputs);
    }

    public @NotNull List<Input> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    public @NotNull List<Output> getOutputs() {
        return Collections.unmodifiableList(outputs);
    }

    /**
     * Whether the transaction is a coinbase transaction.
     * No inputs are allowed, and only one output.
     *
     * @return Whether the transaction is a coinbase transaction.
     */
    public boolean isCoinbase() {
        return inputs.size() == 0 && outputs.size() == 1;
    }

    /**
     * Gets the transaction hash using lazy computation if not available.
     * <p>
     * The hash of a block is the hashed output of the full transaction data.
     * The hash is computed by applying the SHA-256 hashing function twice.
     *
     * @return The transaction hash.
     */
    public synchronized @NotNull Hash getHash() {
        if (hash == null) {
            ByteString data = getRawData();
            hash = Hashing.digestSHA256(Hashing.digestSHA256(data));
        }
        return hash;
    }


    private @NotNull ByteString getRawData() {
        return ProtoConverter.toProto(
            new UnsignedTransaction(this),
            BrabocoinProtos.UnsignedTransaction.class
        ).toByteString();
    }

    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    /**
     * Get the ByteString of this transaction without signature.
     *
     * @return ByteString of this transaction without signature.
     */
    public @NotNull ByteString getSignableTransactionData() {
        return getRawData();
    }

    public @NotNull Transaction sign(List<Signature> signatures) {
        return new Transaction(inputs, outputs, signatures);
    }

    @ProtoClass(BrabocoinProtos.UnsignedTransaction.class)
    public static class Builder implements ProtoBuilder<UnsignedTransaction> {

        @ProtoField
        protected List<Input.Builder> inputs;

        @ProtoField
        protected List<Output.Builder> outputs;

        public Builder setInputs(List<Input.Builder> inputs) {
            this.inputs = inputs;
            return this;
        }

        public Builder setOutputs(List<Output.Builder> outputs) {
            this.outputs = outputs;
            return this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public UnsignedTransaction build() {
            return new UnsignedTransaction(
                inputs.stream().map(Input.Builder::build).collect(Collectors.toList()),
                outputs.stream().map(Output.Builder::build).collect(Collectors.toList())
            );
        }
    }
}
