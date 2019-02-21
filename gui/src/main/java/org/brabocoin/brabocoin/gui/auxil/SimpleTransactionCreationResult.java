package org.brabocoin.brabocoin.gui.auxil;

import org.brabocoin.brabocoin.model.Hash;

public class SimpleTransactionCreationResult {

    private final Hash address;
    private final Hash changeAddress;
    private final long amount;
    private final long fee;

    public SimpleTransactionCreationResult(Hash address, Hash changeAddress, long amount, long fee) {
        this.address = address;
        this.changeAddress = changeAddress;
        this.amount = amount;
        this.fee = fee;
    }

    public Hash getAddress() {
        return address;
    }

    public long getAmount() {
        return amount;
    }

    public long getFee() {
        return fee;
    }

    public Hash getChangeAddress() {
        return changeAddress;
    }
}
