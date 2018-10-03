package org.brabocoin.brabocoin.model;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;

/**
 * Implementation of an output of a transaction
 */
@ProtoClass(BrabocoinProtos.Output.class)
public class Output {
    /**
     * Address of the receiver of the output amount.
     */
    @ProtoField
    private final Hash address;

    /**
     * Amount paid to the output receiver.
     */
    @ProtoField
    private final long amount;

    /**
     * Create a new output
     * @param address address of the receiver of the output amount
     * @param amount amount paid to the output receiver
     */
    public Output(Hash address, long amount) {
        this.address = address;
        this.amount = amount;
    }

    public Hash getAddress() {
        return address;
    }

    public long getAmount() {
        return amount;
    }

    @ProtoClass(BrabocoinProtos.Output.class)
    public static class Builder {
        @ProtoField
        private Hash.Builder address;
        @ProtoField
        private long amount;

        public Builder setAddress(Hash.Builder address) {
            this.address = address;
            return this;
        }

        public Builder setAmount(long amount) {
            this.amount = amount;
            return this;
        }

        public Output createOutput() {
            return new Output(address.createHash(), amount);
        }
    }
}
