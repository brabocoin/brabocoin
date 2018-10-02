package org.brabocoin.brabocoin.model;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;

import java.util.List;

/**
 * Implementation of transaction.
 */
@ProtoClass(BrabocoinProtos.Transaction.class)
public class Transaction {

    /**
     * Inputs used by the transaction.
     */
    @ProtoField
    private final List<Input> inputs;

    /**
     * Outputs used by the transaction.
     */
    @ProtoField
    private final List<Output> outputs;

    /**
     * Create a new transaction.
     * @param inputs inputs used by the transaction.
     * @param outputs outputs used by the transaction.
     */
    public Transaction(List<Input> inputs, List<Output> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public List<Input> getInputs() {
        return inputs;
    }

    public List<Output> getOutputs() {
        return outputs;
    }
}
