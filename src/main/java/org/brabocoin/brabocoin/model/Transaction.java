package org.brabocoin.brabocoin.model;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transaction with brabocoin inputs and outputs.
 */
@ProtoClass(BrabocoinProtos.Transaction.class)
public class Transaction {

    /**
     * Inputs used by the transaction.
     */
    @ProtoField
    private final @NotNull List<Input> inputs;

    /**
     * Outputs used by the transaction.
     */
    @ProtoField
    private final @NotNull List<Output> outputs;

    /**
     * Create a new transaction.
     *
     * @param inputs
     *         inputs used by the transaction.
     * @param outputs
     *         outputs used by the transaction.
     */
    public Transaction(@NotNull List<Input> inputs, @NotNull List<Output> outputs) {
        this.inputs = new ArrayList<>(inputs);
        this.outputs = new ArrayList<>(outputs);
    }

    public @NotNull List<Input> getInputs() {
        return inputs;
    }

    public @NotNull List<Output> getOutputs() {
        return outputs;
    }

    @ProtoClass(BrabocoinProtos.Transaction.class)
    public static class Builder {

        @ProtoField
        private List<Input.Builder> inputs;
        @ProtoField
        private List<Output.Builder> outputs;

        public Builder setInputs(List<Input.Builder> inputs) {
            this.inputs = inputs;
            return this;
        }

        public Builder setOutputs(List<Output.Builder> outputs) {
            this.outputs = outputs;
            return this;
        }

        public Transaction createTransaction() {
            return new Transaction(
                    inputs.stream()
                            .map(Input.Builder::createInput)
                            .collect(Collectors.toList()),
                    outputs.stream()
                            .map(Output.Builder::createOutput)
                            .collect(Collectors.toList())
            );
        }
    }
}
