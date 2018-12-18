package org.brabocoin.brabocoin.model;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.model.crypto.Signature;
import org.brabocoin.brabocoin.crypto.Hashing;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transaction with brabocoin inputs and outputs, including signatures for the inputs.
 */
@ProtoClass(BrabocoinProtos.Transaction.class)
public class Transaction extends UnsignedTransaction {

    @ProtoField
    private final @NotNull List<Signature> signatures;

    /**
     * Create a transaction from an unsigned transaction.
     *
     * @param unsignedTransaction
     *     The unsigned transaction.
     * @param signatures
     *     The signatures to sign with.
     * @return The signed transaction.
     */
    public static @NotNull Transaction fromUnsigned(
        @NotNull UnsignedTransaction unsignedTransaction,
        @NotNull List<Signature> signatures) {
        return unsignedTransaction.sign(signatures);
    }

    private static Input createCoinbaseInput(int blockHeight) {
        return new Input(
            coinbaseReferencedTransaction,
            blockHeight
        );
    }

    public static @NotNull Transaction coinbase(@NotNull Output output, int blockHeight) {
        return new Transaction(
            Collections.singletonList(createCoinbaseInput(blockHeight)),
            Collections.singletonList(output),
            Collections.emptyList()
        );
    }

    /**
     * Create a new transaction.
     *
     * @param inputs
     *     Inputs used by the transaction.
     * @param outputs
     *     Outputs used by the transaction.
     * @param signatures
     *     The list of signatures of the transaction.
     */
    public Transaction(@NotNull List<Input> inputs, @NotNull List<Output> outputs,
                       @NotNull List<Signature> signatures) {
        super(inputs, outputs);
        this.signatures = new ArrayList<>(signatures);
    }

    public @NotNull List<Signature> getSignatures() {
        return Collections.unmodifiableList(signatures);
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    @ProtoClass(BrabocoinProtos.Transaction.class)
    public static class Builder extends UnsignedTransaction.Builder {

        @ProtoField
        private List<Signature.Builder> signatures;

        public Builder setSignatures(List<Signature.Builder> signatures) {
            this.signatures = signatures;
            return this;
        }

        @Override
        public Transaction build() {
            return new Transaction(
                inputs.stream().map(Input.Builder::build).collect(Collectors.toList()),
                outputs.stream().map(Output.Builder::build).collect(Collectors.toList()),
                signatures.stream().map(Signature.Builder::build).collect(Collectors.toList())
            );
        }
    }

    @Override
    public @NotNull
    synchronized Hash getHash() {
        if (hash == null) {
            ByteString data = ProtoConverter.toProto(
                this,
                BrabocoinProtos.Transaction.class
            ).toByteString();
            hash = Hashing.digestSHA256(Hashing.digestSHA256(data));
        }
        return hash;
    }
}
