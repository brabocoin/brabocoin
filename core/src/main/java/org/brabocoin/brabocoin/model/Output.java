package org.brabocoin.brabocoin.model;

import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.jetbrains.annotations.NotNull;

/**
 * Output of a transaction.
 */
@ProtoClass(BrabocoinProtos.Output.class)
public class Output implements ProtoModel<Output> {

    /**
     * Address of the receiver of the output amount.
     */
    @ProtoField
    private final @NotNull Hash address;

    /**
     * Amount paid to the output receiver.
     */
    @ProtoField
    private final long amount;

    /**
     * Create a new output.
     *
     * @param address
     *     Address of the receiver of the output amount.
     * @param amount
     *     Amount paid to the output receiver.
     */
    public Output(@NotNull Hash address, long amount) {
        this.address = address;
        this.amount = amount;
    }

    public @NotNull Hash getAddress() {
        return address;
    }

    public long getAmount() {
        return amount;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    @ProtoClass(BrabocoinProtos.Output.class)
    public static class Builder implements ProtoBuilder<Output> {

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

        public Output build() {
            return new Output(address.build(), amount);
        }
    }
}
